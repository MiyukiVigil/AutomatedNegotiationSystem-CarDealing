package org.example.agents;

import java.util.ArrayList;
import java.util.List;
import org.example.MainUI.UILogger;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * BuyerAgent — Phase 3
 *
 * Negotiation is now fully broker-routed. The buyer NEVER contacts a dealer directly.
 *
 * Flow:
 *  1. Send BUYER_SEARCH (REQUEST) to broker with sessionId + carModel
 *  2. Receive BROKER_SHORTLIST (PROPOSE) — parse dealers
 *  3. Pick next unaffordable-skipped dealer; send BUYER_SHORTLIST (PROPOSE) to broker
 *     (broker creates session, charges fixed fee, invites dealer)
 *  4. Receive BROKER_RELAY_COUNTER (REJECT_PROPOSAL) — send BUYER_COUNTER (PROPOSE) to broker
 *  5. Receive BROKER_RELAY_ACCEPT (ACCEPT_PROPOSAL) — deal done
 *  6. If max rounds reached → BUYER_WALKAWAY (FAILURE) to broker → try next dealer
 *
 * Session IDs: "{buyerLocalName}-{carModel}-{attemptIndex}"
 * Each dealer attempt is a new session (new fixed fee).
 *
 * Represents a buyer that searches and negotiates only through the broker.
 */
public class BuyerAgent extends Agent {

    // ── Config ────────────────────────────────────────────────────────────────
    private String desiredCar;
    private int    maxBudget;
    private UILogger logger;
    private NegotiationConfig config = NegotiationConfig.defaults();
    private UtilityPreferences preferences = AppConfig.defaults().utilityPreferences();
    private boolean negotiationStarted = true;  // false = wait for manual start signal
    private boolean isManualStrategy = false;
    private static final int MAX_DEALER_NEGOTIATIONS = 3;
    private static final int NARROW_PRICE_WINDOW = 10_000;

    // ── Negotiation state ─────────────────────────────────────────────────────
    private final List<DealerOption> dealers = new ArrayList<>();
    private int    currentDealerIdx   = 0;
    private int    dealerAttemptIndex = 0;  // increments per dealer attempt → unique sessionIds
    private String activeSessionId    = null;

    private int    negotiationRound       = 0;
    private int    startCycle             = -1;
    private int    elapsedCycle           = 0;
    private int    searchRetries          = 0;
    private boolean dealFound             = false;
    private int    activeDealerListedPrice = -1;

    private int    initialOffer;
    private int    currentWillingOffer;
    private NegotiationConfig.Strategy activeStrategy;
    private NegotiationTerms currentTerms;
    private boolean negotiationPaused = false;
    private final List<ACLMessage> pausedMessages = new ArrayList<>();
    private final List<Runnable> pausedActions = new ArrayList<>();

    // ── Inner types ───────────────────────────────────────────────────────────
    /** Shortlisted dealer candidate returned by the broker. */
    private static class DealerOption {
        String name;
        int listedPrice;
        int reservePrice;
        /** Stores dealer name, list price, and reserve price for ranking. */
        DealerOption(String n, int listed, int reserve) {
            name = n; listedPrice = listed; reservePrice = reserve;
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    /** Initializes buyer preferences, registration behavior, and message handling. */
    protected void setup() {
        Object[] args = getArguments();
        desiredCar = (String) args[0];
        maxBudget  = Integer.parseInt((String) args[1]);
        logger     = (UILogger) args[2];
        if (args.length > 3 && args[3] instanceof NegotiationConfig) config = (NegotiationConfig) args[3];
        if (args.length > 4 && args[4] instanceof Boolean) negotiationStarted = !((Boolean) args[4]);
        if (args.length > 5 && args[5] instanceof Boolean) isManualStrategy = (Boolean) args[5];

        initialOffer       = (int)(maxBudget * config.getBuyerStartPercent());
        currentWillingOffer = initialOffer;
        currentTerms = buyerTermsForPrice(currentWillingOffer);
        activeStrategy     = config.getStrategy();

        log("STATUS: " + (negotiationStarted ? "Searching" : "Waiting to start")
                + " for " + desiredCar + " (Budget: RM" + maxBudget
                + ", Strategy: " + config.getStrategy() + strategySwitchText() + ")");

        // Kick off search if auto-start
        addBehaviour(new OneShotBehaviour() {
            /** Starts the search immediately unless the UI requested manual start. */
            @Override public void action() {
                if (negotiationStarted) startNegotiationSession();
            }
        });

        // Main message loop
        addBehaviour(new CyclicBehaviour() {
            /** Dispatches broker, space, and manual-control ACL messages. */
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                String ont = msg.getOntology() == null ? "" : msg.getOntology();

                if ("PAUSE_NEGOTIATION".equals(ont)) {
                    negotiationPaused = true;
                    log("STATUS: Negotiation paused.");
                    return;
                }
                if ("RESUME_NEGOTIATION".equals(ont)) {
                    negotiationPaused = false;
                    log("STATUS: Negotiation resumed.");
                    resumePausedWork();
                    return;
                }
                if (negotiationPaused && isPausableMessage(ont)) {
                    pausedMessages.add((ACLMessage) msg.clone());
                    return;
                }

                switch (ont) {
                    case "START_NEGOTIATION":
                        if (!negotiationStarted) startNegotiationSession();
                        else log("STATUS: Start ignored — negotiation already running.");
                        break;

                    case "STOP_NEGOTIATION":
                        log("STATUS: Negotiation stopped by user.");
                        sendWalkaway(activeSessionId, "USER_STOPPED");
                        triggerMarketAction();
                        doDelete();
                        break;

                    case "CYCLE_UPDATE":
                        if (negotiationStarted) handleCycleUpdate(msg);
                        break;

                    case "BROKER_SHORTLIST":
                        if (negotiationStarted) handleBrokerShortlist(msg);
                        break;

                    case "BROKER_RELAY_COUNTER":
                        // Broker relays dealer's counter-offer to us
                        if (negotiationStarted) handleBrokerRelayCounter(msg);
                        break;

                    case "BROKER_RELAY_ACCEPT":
                        // Broker confirms deal
                        if (negotiationStarted) handleBrokerRelayAccept(msg);
                        break;

                    case "BROKER_RELAY_SOLD_OUT":
                        // Broker tells us the dealer ran out of stock — move to next dealer
                        if (negotiationStarted) handleBrokerRelaySoldOut(msg);
                        break;

                    case "BROKER_SESSION_REJECTED":
                        if (negotiationStarted) handleBrokerSessionRejected(msg);
                        break;

                    case "MANUAL_ACTION":
                        handleManualAction(msg);
                        break;

                    default:
                        break;
                }
            }
        });
    }

    // ── Session management ────────────────────────────────────────────────────

    /** Registers with SpaceControl and begins the broker search phase. */
    private void startNegotiationSession() {
        negotiationStarted = true;
        log("STATUS: Negotiation started for " + desiredCar + ".");

        // Register with SpaceControl
        ACLMessage reg = new ACLMessage(ACLMessage.INFORM);
        reg.setOntology("REGISTER");
        reg.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(reg);

        searchBroker();
    }

    /** Send BUYER_SEARCH to broker. Uses a placeholder sessionId for the search phase. */
    public void searchBroker() {
        // Session ID for the search itself (not a negotiation session yet)
        String searchId = getLocalName() + "-" + desiredCar + "-search";
        ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
        req.addReceiver(new AID("broker", AID.ISLOCALNAME));
        req.setOntology("BUYER_SEARCH");
        req.setContent(searchId + ";" + desiredCar);
        send(req);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    /** Updates the buyer's willing offer after a simulation cycle. */
    private void handleCycleUpdate(ACLMessage msg) {
        int currentCycle = Integer.parseInt(msg.getContent());
        if (startCycle == -1) startCycle = currentCycle;

        elapsedCycle = Math.min(Math.max(0, currentCycle - startCycle), config.getDeadlineCycles());
        NegotiationConfig.Strategy effective = config.getEffectiveStrategy(elapsedCycle);
        if (effective != activeStrategy) {
            activeStrategy = effective;
            log("STATUS: Strategy shifted to " + activeStrategy + " at local cycle " + elapsedCycle);
        }
        currentWillingOffer = Math.max(currentWillingOffer, calculateWillingOffer(elapsedCycle));
        currentTerms = buyerTermsForPrice(currentWillingOffer);
        log("Willing to pay RM" + currentWillingOffer + " for " + desiredCar
                + termsText(currentTerms));
    }

    /**
     * BROKER_SHORTLIST: "searchSessionId;dealer1:price1:reserve1,dealer2:..."  or  "...;NONE"
     * Parse shortlist, verify affordability, send BUYER_SHORTLIST to broker.
     */
    private void handleBrokerShortlist(ACLMessage msg) {
        // Content: "{searchId};{dealerCsv}"  where dealerCsv may be "NONE"
        String content = msg.getContent();
        int    sep     = content.indexOf(';');
        String dealerCsv = sep >= 0 ? content.substring(sep + 1) : content;

        if ("NONE".equals(dealerCsv)) {
            searchRetries++;
            if (searchRetries > config.getMaxSearchRetries()) {
                log("STATUS: No matching car after " + config.getMaxSearchRetries() + " retries. Ending.");
                sendNoDealReport("NO_MATCHING_CAR");
                triggerMarketAction();
                doDelete();
            } else {
                log("STATUS: No dealers for " + desiredCar + ". Retry " + searchRetries
                        + "/" + config.getMaxSearchRetries());
                addBehaviour(new WakerBehaviour(this, 1500) {
                    /** Retries broker search after a short delay. */
                    @Override protected void onWake() { runWhenResumed(() -> searchBroker()); }
                });
            }
            return;
        }

        dealers.clear();
        for (String entry : dealerCsv.split(",")) {
            if (entry.isEmpty()) continue;
            String[] p = entry.split(":");
            int listed  = p.length > 1 ? Integer.parseInt(p[1]) : maxBudget;
            int reserve = p.length > 2 ? Integer.parseInt(p[2]) : listed;
            dealers.add(new DealerOption(p[0], listed, reserve));
        }
        dealers.sort((a, b) -> Integer.compare(a.listedPrice, b.listedPrice));
        while (dealers.size() > MAX_DEALER_NEGOTIATIONS) {
            dealers.remove(dealers.size() - 1);
        }

        // Check at least one dealer is affordable
        boolean anyAffordable = dealers.stream().anyMatch(d -> d.reservePrice <= maxBudget);
        if (!anyAffordable) {
            log("STATUS: All dealers' reserve prices exceed budget RM" + maxBudget + ". Ending.");
            sendNoDealReport("BUDGET_TOO_LOW");
            triggerMarketAction();
            doDelete();
            return;
        }

        searchRetries = 0;
        currentDealerIdx = 0;
        dealerAttemptIndex = 0;
        
        if (isManualStrategy) {
            log("STATUS: Manual Shortlist Phase. Waiting for user input...");
            StringBuilder sb = new StringBuilder();
            for (DealerOption d : dealers) {
                if (d.reservePrice <= maxBudget) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(d.name).append(":").append(d.listedPrice).append(":").append(d.reservePrice);
                }
            }
            log("[MANUAL_PROMPT] SHORTLIST:" + sb.toString());
            return;
        }

        log("STATUS: Shortlist received — " + dealers.size() + " dealer(s). Starting negotiations...");
        submitShortlistToBroker();
    }

    /**
     * Pick the current dealer (skip if unaffordable), generate a new sessionId,
     * and send BUYER_SHORTLIST to broker.
     */
    private void submitShortlistToBroker() {
        // Skip dealers whose reserve price exceeds budget
        while (currentDealerIdx < dealers.size()
                && dealers.get(currentDealerIdx).reservePrice > maxBudget) {
            log("STATUS: Skipping " + dealers.get(currentDealerIdx).name
                    + " (reserve RM" + dealers.get(currentDealerIdx).reservePrice + " > budget)");
            currentDealerIdx++;
        }

        if (dealerAttemptIndex >= MAX_DEALER_NEGOTIATIONS || currentDealerIdx >= dealers.size()) {
            if (!dealFound) {
                log("STATUS: Dealer negotiation limit reached. No deal reached.");
                if (dealerAttemptIndex == 0) {
                    sendNoDealReport("MAX_ROUNDS_REACHED");
                }
                triggerMarketAction();
                doDelete();
            }
            return;
        }

        DealerOption dealer = dealers.get(currentDealerIdx);
        negotiationRound = 0;
        activeDealerListedPrice = dealer.listedPrice;
        activeSessionId  = getLocalName() + "-" + desiredCar + "-" + dealerAttemptIndex;

        NegotiationTerms firstTerms = buyerTermsForPrice(currentWillingOffer);

        // Content: "sessionId;dealerName;terms;buyerReserve;carModel"
        ACLMessage shortlist = new ACLMessage(ACLMessage.PROPOSE);
        shortlist.addReceiver(new AID("broker", AID.ISLOCALNAME));
        shortlist.setOntology("BUYER_SHORTLIST");
        shortlist.setContent(activeSessionId + ";" + dealer.name + ";"
                + firstTerms.toPayload() + ";" + maxBudget + ";" + desiredCar);
        
        addBehaviour(new WakerBehaviour(this, 400) {
            /** Sends the shortlist after a short delay for readable demo pacing. */
            @Override
            protected void onWake() {
                runWhenResumed(() -> send(shortlist));
            }
        });

        log("NEGOTIATION: Starting with " + dealer.name + " [session=" + activeSessionId
                + "] @ RM" + currentWillingOffer + termsText(firstTerms));
    }

    /**
     * BROKER_RELAY_COUNTER: "sessionId;dealerName;counterPrice"
     * The broker has relayed the dealer's counter-offer to us.
     */
    private void handleBrokerRelayCounter(ACLMessage msg) {
        String[] p   = msg.getContent().split(";");
        String sid   = p[0];
        // p[1] = dealerName
        NegotiationTerms counterTerms = NegotiationTerms.fromPayload(p[2]);
        int counter  = counterTerms.getPrice();
        negotiationRound++;

        log("OFFER: [" + sid + "] Dealer counter-offered RM" + counter
                + termsText(counterTerms) + " (Round " + negotiationRound + ")");

        if (isManualStrategy) {
            log("[MANUAL_PROMPT] COUNTER:" + p[1] + ":" + counter);
            return;
        }

        if (shouldAcceptCounter(counterTerms)) {
            // Deal! Send agreeing counter back via broker (broker will forward to dealer)
            sendBuyerCounter(sid, counterTerms);
            log("AGREED: Counter RM" + counter + " is within budget. Sending final offer.");
            // Note: the actual deal confirmation comes from DEALER_ACCEPT → BROKER_RELAY_ACCEPT
        } else if (negotiationRound < config.getMaxRoundsPerDealer()) {
            moveOfferForNegotiationRound();
            // Acceleration if stuck
            if (negotiationRound >= config.getStuckRoundsBeforeAcceleration()) {
                currentWillingOffer = Math.min(maxBudget,
                        currentWillingOffer + Math.max(1, (maxBudget - currentWillingOffer) / 2));
                currentTerms = buyerTermsForPrice(currentWillingOffer);
                log("STATUS: Negotiation dragging. Accelerated offer to RM" + currentWillingOffer);
            }
            sendBuyerCounter(sid, buyerTermsForPrice(currentWillingOffer));
            log("COUNTER: Sent RM" + currentWillingOffer + termsText(currentTerms) + " to broker.");
            triggerMarketAction();
        } else {
            // Walk away from this dealer, try next
            log("STATUS: Max rounds reached on " + sid + ". Moving to next dealer...");
            sendWalkaway(sid, "MAX_ROUNDS_REACHED");
            currentDealerIdx++;
            dealerAttemptIndex++;
            submitShortlistToBroker();
        }
    }

    /**
     * BROKER_RELAY_ACCEPT: "sessionId;dealerName;agreedPrice"
     * The deal is confirmed by the broker (dealer already accepted).
     */
    private void handleBrokerRelayAccept(ACLMessage msg) {
        String[] p   = msg.getContent().split(";");
        // p[0]=sessionId, p[1]=dealerName
        NegotiationTerms agreedTerms = NegotiationTerms.fromPayload(p[2]);
        int agreedPrice = agreedTerms.getPrice();
        log("SUCCESS! Purchased " + desiredCar + " for RM" + agreedPrice
                + termsText(agreedTerms) + " from " + p[1] + " [session=" + p[0] + "]");
        dealFound = true;
        triggerMarketAction();
        doDelete();
    }

    /**
     * BROKER_RELAY_SOLD_OUT: "sessionId;dealerName"
     * The dealer went out of stock mid-negotiation.
     * Advance to the next dealer in the shortlist immediately.
     */
    private void handleBrokerRelaySoldOut(ACLMessage msg) {
        String[] p      = msg.getContent().split(";", 2);
        String sessionId = p[0];
        String dealer   = p.length > 1 ? p[1] : "?";
        log("STATUS: Dealer " + dealer + " sold out [session=" + sessionId + "]. Trying next dealer...");
        currentDealerIdx++;
        dealerAttemptIndex++;
        submitShortlistToBroker();
    }

    // ── Outbound helpers ──────────────────────────────────────────────────────

    /** Handles broker rejection or timeout and advances to the next dealer when possible. */
    private void handleBrokerSessionRejected(ACLMessage msg) {
        String[] p = msg.getContent().split(";", 2);
        String sid = p.length > 0 ? p[0] : activeSessionId;
        String reason = p.length > 1 ? p[1] : "UNKNOWN";
        log("STATUS: Broker closed/rejected session " + sid + " (" + reason + ").");
        if ("TIMEOUT".equals(reason)) {
            triggerMarketAction();
            doDelete();
            return;
        }
        currentDealerIdx++;
        dealerAttemptIndex++;
        submitShortlistToBroker();
    }

    /** Send BUYER_COUNTER (PROPOSE / BUYER_COUNTER) to broker */
    private void sendBuyerCounter(String sessionId, NegotiationTerms terms) {
        ACLMessage counter = new ACLMessage(ACLMessage.PROPOSE);
        counter.addReceiver(new AID("broker", AID.ISLOCALNAME));
        counter.setOntology("BUYER_COUNTER");
        counter.setContent(sessionId + ";" + terms.toPayload());
        
        addBehaviour(new WakerBehaviour(this, 600) {
            /** Sends the buyer counter after a short delay for readable demo pacing. */
            @Override
            protected void onWake() {
                runWhenResumed(() -> send(counter));
            }
        });
    }

    /** Returns true for messages that should wait while the simulation is paused. */
    private boolean isPausableMessage(String ontology) {
        return "BROKER_SHORTLIST".equals(ontology)
                || "BROKER_RELAY_COUNTER".equals(ontology)
                || "BROKER_RELAY_ACCEPT".equals(ontology)
                || "BROKER_RELAY_SOLD_OUT".equals(ontology)
                || "BROKER_SESSION_REJECTED".equals(ontology)
                || "MANUAL_ACTION".equals(ontology);
    }

    /** Runs a delayed action immediately, or holds it until the simulation resumes. */
    private void runWhenResumed(Runnable action) {
        if (negotiationPaused) {
            pausedActions.add(action);
            return;
        }
        action.run();
    }

    /** Replays held delayed actions and inbound messages when the simulation resumes. */
    private void resumePausedWork() {
        while (!pausedActions.isEmpty() && !negotiationPaused) {
            pausedActions.remove(0).run();
        }
        while (!pausedMessages.isEmpty() && !negotiationPaused) {
            handleHeldMessage(pausedMessages.remove(0));
        }
    }

    /** Dispatches a message that arrived while paused. */
    private void handleHeldMessage(ACLMessage msg) {
        String ont = msg.getOntology() == null ? "" : msg.getOntology();
        switch (ont) {
            case "BROKER_SHORTLIST":
                if (negotiationStarted) handleBrokerShortlist(msg);
                break;
            case "BROKER_RELAY_COUNTER":
                if (negotiationStarted) handleBrokerRelayCounter(msg);
                break;
            case "BROKER_RELAY_ACCEPT":
                if (negotiationStarted) handleBrokerRelayAccept(msg);
                break;
            case "BROKER_RELAY_SOLD_OUT":
                if (negotiationStarted) handleBrokerRelaySoldOut(msg);
                break;
            case "BROKER_SESSION_REJECTED":
                if (negotiationStarted) handleBrokerSessionRejected(msg);
                break;
            case "MANUAL_ACTION":
                handleManualAction(msg);
                break;
            default:
                break;
        }
    }

    /** Send BUYER_WALKAWAY (FAILURE / BUYER_WALKAWAY) to broker */
    private void sendWalkaway(String sessionId, String reason) {
        if (sessionId == null) {
            sendNoDealReport(reason);
            return;
        }
        ACLMessage walkaway = new ACLMessage(ACLMessage.FAILURE);
        walkaway.addReceiver(new AID("broker", AID.ISLOCALNAME));
        walkaway.setOntology("BUYER_WALKAWAY");
        walkaway.setContent(sessionId + ";" + reason);
        send(walkaway);
    }

    /** Reports a pre-session no-deal outcome to the broker for visible metrics. */
    private void sendNoDealReport(String reason) {
        String reportId = getLocalName() + "-" + desiredCar + "-pre-session-" + reason;
        ACLMessage walkaway = new ACLMessage(ACLMessage.FAILURE);
        walkaway.addReceiver(new AID("broker", AID.ISLOCALNAME));
        walkaway.setOntology("BUYER_WALKAWAY");
        walkaway.setContent(reportId + ";" + reason + ";" + desiredCar + ";" + maxBudget);
        send(walkaway);
    }

    /** Notifies SpaceControl that the buyer completed an action this cycle. */
    private void triggerMarketAction() {
        ACLMessage action = new ACLMessage(ACLMessage.INFORM);
        action.setOntology("ACTION_COMPLETED");
        action.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(action);
    }

    /** Executes a GUI-driven manual negotiation command for this buyer. */
    private void handleManualAction(ACLMessage msg) {
        String content = msg.getContent();
        if (content == null || content.isEmpty()) return;
        
        String[] parts = content.split(";");
        String action = parts[0];
        
        if ("SHORTLIST".equals(action) && parts.length >= 3) {
            String dealerName = parts[1];
            int firstOffer = Integer.parseInt(parts[2]);
            if (dealerName == null || dealerName.trim().isEmpty() || firstOffer <= 0) {
                log("MANUAL: Select a dealer and enter a positive first offer.");
                return;
            }
            
            activeSessionId = getLocalName() + "-" + desiredCar + "-" + dealerAttemptIndex;
            ACLMessage prop = new ACLMessage(ACLMessage.PROPOSE);
            prop.addReceiver(new AID("broker", AID.ISLOCALNAME));
            prop.setOntology("BUYER_SHORTLIST");
            prop.setContent(activeSessionId + ";" + dealerName + ";" + buyerTermsForPrice(firstOffer).toPayload()
                    + ";" + maxBudget + ";" + desiredCar);
            send(prop);
            
            log("MANUAL: Sent first offer RM" + firstOffer + " to " + dealerName);
            
        } else if ("COUNTER".equals(action) && parts.length >= 2) {
            int price = Integer.parseInt(parts[1]);
            if (activeSessionId == null || price <= 0) {
                log("MANUAL: Counter requires an active session and a positive price.");
                return;
            }
            ACLMessage counterMsg = new ACLMessage(ACLMessage.PROPOSE);
            counterMsg.addReceiver(new AID("broker", AID.ISLOCALNAME));
            counterMsg.setOntology("BUYER_COUNTER");
            counterMsg.setContent(activeSessionId + ";" + buyerTermsForPrice(price).toPayload());
            send(counterMsg);
            log("MANUAL: Sent counter offer RM" + price);
            
        } else if ("ACCEPT".equals(action) && parts.length >= 2) {
            int price = Integer.parseInt(parts[1]);
            if (activeSessionId == null || price <= 0) {
                log("MANUAL: Accept requires an active session and a positive price.");
                return;
            }
            ACLMessage counterMsg = new ACLMessage(ACLMessage.PROPOSE);
            counterMsg.addReceiver(new AID("broker", AID.ISLOCALNAME));
            counterMsg.setOntology("BUYER_COUNTER");
            counterMsg.setContent(activeSessionId + ";" + buyerTermsForPrice(price).toPayload());
            send(counterMsg);
            log("MANUAL: Sent acceptance price RM" + price);
            
        } else if ("WALKAWAY".equals(action)) {
            sendWalkaway(activeSessionId, "MANUAL_WALKAWAY");
            log("MANUAL: Walked away from negotiation.");
            triggerMarketAction();
            doDelete();
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    /** Deregisters from SpaceControl when the buyer leaves the simulation. */
    protected void takeDown() {
        ACLMessage dereg = new ACLMessage(ACLMessage.INFORM);
        dereg.setOntology("DEREGISTER");
        dereg.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(dereg);
        log("Terminating");
    }

    /** Writes a buyer-prefixed message to the UI logger. */
    private void log(String m) {
        if (logger != null) logger.log(getLocalName() + ": " + m);
    }

    /** Describes the configured strategy switch for status logs. */
    private String strategySwitchText() {
        if (config.getStrategySwitchCycle() <= 0 || config.getSwitchStrategy() == config.getStrategy()) return "";
        return " → " + config.getSwitchStrategy() + " at cycle " + config.getStrategySwitchCycle();
    }

    /** Builds buyer terms for a price using improving warranty and delivery attributes. */
    private NegotiationTerms buyerTermsForPrice(int price) {
        double progress = (double) Math.max(0, price - initialOffer) / Math.max(1, maxBudget - initialOffer);
        int defaultWarranty = preferences.getDefaultWarrantyMonths();
        int warranty = defaultWarranty + (int) Math.round(defaultWarranty * progress);
        int defaultDelivery = preferences.getDefaultDeliveryDays();
        int delivery = Math.max(3, defaultDelivery - (int) Math.round((defaultDelivery - 3) * progress));
        return new NegotiationTerms(price, warranty, delivery);
    }

    /** Calculates the buyer's willing offer for a progress step. */
    private int calculateWillingOffer(int progressStep) {
        int effectiveDeadline = effectiveBuyerDeadlineCycles();
        int boundedStep = Math.min(Math.max(0, progressStep), effectiveDeadline);
        int strategyStep = scaleProgressToConfigCycle(boundedStep, effectiveDeadline);
        double concession = Math.pow((double) boundedStep / effectiveDeadline,
                config.betaForCycle(strategyStep));
        return Math.min(maxBudget, (int) Math.round(initialOffer + ((maxBudget - initialOffer) * concession)));
    }

    /** Advances the buyer offer when a negotiation round occurs. */
    private void moveOfferForNegotiationRound() {
        int progressStep = Math.max(elapsedCycle, negotiationRound);
        int roundBasedOffer = calculateWillingOffer(progressStep);
        if (roundBasedOffer <= currentWillingOffer && currentWillingOffer < maxBudget) {
            roundBasedOffer = currentWillingOffer + Math.max(1, (maxBudget - initialOffer) / effectiveBuyerDeadlineCycles());
        }
        currentWillingOffer = Math.min(maxBudget, roundBasedOffer);
        currentTerms = buyerTermsForPrice(currentWillingOffer);
    }

    /** Extends the concession deadline when the affordability window is narrow. */
    private int effectiveBuyerDeadlineCycles() {
        int deadline = config.getDeadlineCycles();
        int affordabilityWindow = activeDealerListedPrice > 0
                ? maxBudget - activeDealerListedPrice
                : maxBudget - initialOffer;
        if (affordabilityWindow >= 0 && affordabilityWindow <= NARROW_PRICE_WINDOW) {
            return Math.max(deadline, deadline * 2);
        }
        return deadline;
    }

    /** Maps an extended deadline progress step back onto the configured strategy cycle. */
    private int scaleProgressToConfigCycle(int progressStep, int effectiveDeadline) {
        if (effectiveDeadline == config.getDeadlineCycles()) {
            return Math.min(progressStep, config.getDeadlineCycles());
        }
        return Math.min(config.getDeadlineCycles(),
                (int) Math.round((double) progressStep * config.getDeadlineCycles() / effectiveDeadline));
    }

    /** Returns true when the dealer counter is acceptable to the buyer. */
    private boolean shouldAcceptCounter(NegotiationTerms counterTerms) {
        int counter = counterTerms.getPrice();
        if (counter <= currentWillingOffer) {
            return true;
        }
        if (counter > maxBudget) {
            return false;
        }
        if (isNarrowPriceWindow(counter)) {
            int minimumProgress = Math.min(3, Math.max(1, config.getMaxRoundsPerDealer() - 1));
            return Math.max(elapsedCycle, negotiationRound) >= minimumProgress;
        }
        if (acceptableUtility(counterTerms)) {
            return negotiationRound >= 2;
        }
        return negotiationRound >= config.getMaxRoundsPerDealer();
    }

    /** Returns true when the listed/counter price is close to the buyer budget. */
    private boolean isNarrowPriceWindow(int counterPrice) {
        int affordabilityWindow = activeDealerListedPrice > 0
                ? maxBudget - activeDealerListedPrice
                : maxBudget - counterPrice;
        return affordabilityWindow >= 0 && affordabilityWindow <= NARROW_PRICE_WINDOW;
    }

    /** Returns true when terms meet the buyer's weighted utility threshold. */
    private boolean acceptableUtility(NegotiationTerms terms) {
        double utility = preferences.buyerUtility(terms, maxBudget,
                preferences.getDefaultWarrantyMonths() * 2,
                preferences.getDefaultDeliveryDays());
        return terms.getPrice() <= maxBudget && utility >= 0.35;
    }

    /** Formats non-price terms for compact status logging. */
    private String termsText(NegotiationTerms terms) {
        return " | Warranty=" + terms.getWarrantyMonths() + " months | Delivery="
                + terms.getDeliveryDays() + " days";
    }
}
