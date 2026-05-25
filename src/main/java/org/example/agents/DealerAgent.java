package org.example.agents;

import jade.core.*;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import org.example.MainUI.UILogger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DealerAgent — Phase 2
 *
 * Negotiation is now fully broker-routed.
 * Inbound ontologies:
 *   BROKER_INVITE      (REQUEST)  — first offer relayed by broker; "sessionId;buyerName;car;offer"
 *   BROKER_RELAY_OFFER (PROPOSE)  — subsequent buyer counter relayed by broker; same schema
 *   CYCLE_UPDATE / START_CYCLE    — price adjustment from SpaceControl
 *   PRICE_ADJUSTMENT              — manual override from GUI
 *
 * Outbound ontologies:
 *   DEALER_COUNTER (REJECT_PROPOSAL) → broker
 *   DEALER_ACCEPT  (ACCEPT_PROPOSAL) → broker
 *
 * Extension 1 foundation: per-session state is isolated via sessionId so one
 * dealer can safely handle multiple concurrent buyers.
 *
 * Represents one dealer listing that evaluates broker-routed buyer offers.
 */
public class DealerAgent extends Agent {

    private String car;
    private int    minPrice;      // reserve price
    private int    retailPrice;
    private int    currentTargetPrice;
    private UILogger logger;
    private int    negotiationCount = 0;
    private NegotiationConfig config = NegotiationConfig.defaults();
    private UtilityPreferences preferences = AppConfig.defaults().utilityPreferences();
    private int    stockCount;
    private int    manualTargetPrice = -1;
    private int    latestCycle = 0;
    private NegotiationConfig.Strategy activeStrategy;
    private final Map<String, DealerSessionState> activeSessions = new LinkedHashMap<>();
    private boolean negotiationPaused = false;
    private final List<ACLMessage> pausedMessages = new ArrayList<>();
    private final List<Runnable> pausedActions = new ArrayList<>();
    // ★ FIXED: Removed hardcoded NARROW_PRICE_WINDOW. Now uses relative 30% of retail price.

    /** Per-session state that isolates concurrent buyers for the same dealer. */
    private static class DealerSessionState {
        String sessionId;
        String buyerName;
        String carModel;
        NegotiationTerms latestTerms;
        int rounds;
        String status;
        // ★ ADDED: Per-session opponent model to track this buyer's offer trend
        OpponentModel buyerModel;

        /** Creates a state record for a newly contacted buyer session. */
        DealerSessionState(String sessionId, String buyerName, String carModel, NegotiationTerms latestTerms) {
            this.sessionId = sessionId;
            this.buyerName = buyerName;
            this.carModel = carModel;
            this.latestTerms = latestTerms;
            this.status = "NEGOTIATING";
            this.buyerModel = new OpponentModel("buyer[" + buyerName + "]"); // ★ ADDED
        }
    }

    @Override
    /** Initializes dealer inventory, registers with the broker, and starts the message loop. */
    protected void setup() {
        Object[] args = getArguments();
        car         = (String) args[0];
        retailPrice = Integer.parseInt((String) args[1]);
        stockCount  = Integer.parseInt((String) args[2]);
        logger      = (UILogger) args[3];
        if (args.length > 4 && args[4] instanceof NegotiationConfig) {
            config = (NegotiationConfig) args[4];
        }
        minPrice           = (int)(retailPrice * config.getDealerReservePercent());
        currentTargetPrice = retailPrice;
        activeStrategy     = config.getStrategy();

        log("STATUS: Listed " + car + " @ RM" + retailPrice + " | Reserve: RM" + minPrice
                + " | Stock: " + stockCount + " | Strategy: " + config.getStrategy() + strategySwitchText());

        // ── Register with broker and SpaceControl ─────────────────────────────
        addBehaviour(new OneShotBehaviour() {
            /** Registers this dealer's inventory and simulation participation. */
            @Override
            public void action() {
                // Register inventory with broker
                ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
                inform.addReceiver(new AID("broker", AID.ISLOCALNAME));
                inform.setContent(car + ";" + retailPrice + ";" + stockCount + ";" + minPrice);
                send(inform);

                // Register with SpaceControl for CYCLE_UPDATE broadcasts
                ACLMessage reg = new ACLMessage(ACLMessage.INFORM);
                reg.setOntology("REGISTER");
                reg.addReceiver(new AID("space", AID.ISLOCALNAME));
                send(reg);
            }
        });

        // ── Main message loop ─────────────────────────────────────────────────
        addBehaviour(new CyclicBehaviour() {
            /** Handles cycle updates, manual price changes, and brokered buyer offers. */
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

                if ("CYCLE_UPDATE".equals(ont) || "START_CYCLE".equals(ont)) {
                    handleCycleUpdate(msg);

                } else if ("PRICE_ADJUSTMENT".equals(ont)) {
                    handleManualPriceAdjustment(msg);

                } else if ("BROKER_INVITE".equals(ont)) {
                    // Content: "sessionId;buyerName;carModel;offer" — first contact for this session
                    handleBrokerOffer(msg);
                } else if ("BROKER_RELAY_OFFER".equals(ont)) {
                    // Content: "sessionId;buyerName;carModel;offer" — subsequent buyer counter
                    handleBrokerOffer(msg);
                }
                // ignore DEREGISTER/ACTION_COMPLETED leaks from other agents
            }
        });
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    /** Applies a CYCLE_UPDATE by recalculating the dealer target price. */
    private void handleCycleUpdate(ACLMessage msg) {
        int currentCycle = Integer.parseInt(msg.getContent());
        latestCycle = Math.min(Math.max(0, currentCycle), config.getDeadlineCycles());

        NegotiationConfig.Strategy effective = config.getEffectiveStrategy(latestCycle);
        if (effective != activeStrategy) {
            activeStrategy = effective;
            log("STATUS: Strategy shifted to " + activeStrategy + " at cycle " + latestCycle);
        }
        currentTargetPrice = Math.min(currentTargetPrice, calculateTargetPrice(latestCycle));
        log("Price updated to RM" + currentTargetPrice + " (cycle " + latestCycle + ")");
    }

    /** Applies a manual target-price override from the GUI. */
    private void handleManualPriceAdjustment(ACLMessage msg) {
        try {
            int adjusted = Integer.parseInt(msg.getContent());
            manualTargetPrice  = Math.max(minPrice, adjusted);
            currentTargetPrice = manualTargetPrice;
            log("STATUS: Manual target adjusted to RM" + currentTargetPrice);
        } catch (NumberFormatException e) {
            log("STATUS: Ignored invalid manual price: " + msg.getContent());
        }
    }

    /** Evaluates BROKER_INVITE or BROKER_RELAY_OFFER content: sessionId;buyer;car;terms. */
    private void handleBrokerOffer(ACLMessage msg) {
        String[] p    = msg.getContent().split(";");
        if (p.length < 4) {
            log("STATUS: Ignored malformed broker offer: " + msg.getContent());
            return;
        }
        String sessionId  = p[0];
        String buyerName = p[1];
        String carModel = p[2];

        if (stockCount <= 0) {
            // Already sold out, reject immediately (zombie window)
            activeSessions.remove(sessionId);
            ACLMessage soldOut = new ACLMessage(ACLMessage.INFORM);
            soldOut.addReceiver(new AID("broker", AID.ISLOCALNAME));
            soldOut.setOntology("DEALER_SOLD_OUT");
            soldOut.setContent(sessionId);
            send(soldOut);
            return;
        }

        NegotiationTerms buyerTerms = NegotiationTerms.fromPayload(p[3]);
        int buyerOffer = buyerTerms.getPrice();
        DealerSessionState state = activeSessions.computeIfAbsent(sessionId,
                id -> new DealerSessionState(id, buyerName, carModel, buyerTerms));
        state.buyerName = buyerName;
        state.carModel = carModel;
        state.latestTerms = buyerTerms;
        state.rounds++;
        negotiationCount++;
        moveTargetForNegotiationRound(state.rounds);

        log("OFFER #" + negotiationCount + ": [" + sessionId + "] Buyer offered RM" + buyerOffer
                + termsText(buyerTerms) + " (target=RM" + currentTargetPrice + ")");

        // ★ ADDED: Record buyer offer into opponent model and log prediction
        state.buyerModel.recordOffer(buyerOffer);
        log("PREDICT: " + state.buyerModel.summary(currentTargetPrice));

        if (state.rounds == 1 && isClearlyUnworkableFirstOffer(buyerTerms)) {
            state.status = "REJECTED";
            activeSessions.remove(sessionId);
            ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            reject.addReceiver(new AID("broker", AID.ISLOCALNAME));
            reject.setOntology("DEALER_REJECT");
            reject.setContent(sessionId + ";DEALER_REJECTED_UNWORKABLE_FIRST_OFFER");
            send(reject);
            log("STATUS: Rejected buyer " + buyerName + " for " + carModel
                    + " because first offer terms are too far below reserve.");
            return;
        }

        if (buyerOffer >= currentTargetPrice || acceptableUtility(buyerTerms)) {
            // Accept — decrement stock and remove this session from active set
            stockCount--;
            state.status = "SETTLED";
            activeSessions.remove(sessionId);

            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            accept.addReceiver(new AID("broker", AID.ISLOCALNAME));
            accept.setOntology("DEALER_ACCEPT");
            accept.setContent(sessionId + ";" + buyerTerms.toPayload());

            // Snapshot remaining sessions at the exact moment of stock-out
            final boolean isStockOutTrigger = (stockCount == 0);
            final String pendingSessionsCsv = (isStockOutTrigger && !activeSessions.isEmpty())
                    ? activeSessions.keySet().stream().collect(Collectors.joining(","))
                    : "";

            if (isStockOutTrigger) {
                // Clear active sessions so we don't accidentally process them further
                activeSessions.clear();
            }

            addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 600) {
                /** Sends the acceptance and any sold-out notices after a short UI-friendly delay. */
                @Override
                protected void onWake() {
                    runWhenResumed(() -> {
                        send(accept);
                        notifySpace();
                        if (isStockOutTrigger) {
                            // Notify broker of all sessions that can no longer be served
                            if (!pendingSessionsCsv.isEmpty()) {
                                ACLMessage soldOut = new ACLMessage(ACLMessage.INFORM);
                                soldOut.addReceiver(new AID("broker", AID.ISLOCALNAME));
                                soldOut.setOntology("DEALER_SOLD_OUT");
                                soldOut.setContent(pendingSessionsCsv);
                                send(soldOut);
                                log("STATUS: Out of stock. Notifying broker of " +
                                        pendingSessionsCsv.split(",").length + " pending session(s).");
                            } else {
                                log("STATUS: Out of stock. No pending sessions.");
                            }
                            doDelete();
                        }
                    });
                }
            });
            log("DEAL CLOSED: [" + sessionId + "] RM" + buyerOffer + termsText(buyerTerms)
                    + " | Stock: " + stockCount);
        } else {
            state.status = "COUNTERED";
            int prevTarget = currentTargetPrice;
            NegotiationTerms counterTerms = dealerCounterTerms();

            // ★ ADDED: Show active strategy and concession amount in log
            String activeStrategyName = config.getEffectiveStrategy(latestCycle).name();
            int concessionAmount = prevTarget - currentTargetPrice;
            String concessionText = concessionAmount > 0 ? " ↓RM" + concessionAmount : "";

            // ★ ADDED: Use opponent model — if buyer predicted to reach our target, hold firm
            int predictedBuyerNext = state.buyerModel.predictNextOffer();
            boolean buyerComingToUs = predictedBuyerNext > 0
                    && predictedBuyerNext >= currentTargetPrice
                    && state.buyerModel.size() >= 2;
            if (buyerComingToUs) {
                log("PREDICT: Buyer predicted to offer RM" + predictedBuyerNext
                        + " next round — holding firm at RM" + currentTargetPrice);
                counterTerms = dealerCounterTerms();
            }

            // Counter-offer
            ACLMessage counter = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            counter.addReceiver(new AID("broker", AID.ISLOCALNAME));
            counter.setOntology("DEALER_COUNTER");
            counter.setContent(sessionId + ";" + counterTerms.toPayload());

            addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 600) {
                /** Sends the counter-offer after a short UI-friendly delay. */
                @Override
                protected void onWake() {
                    runWhenResumed(() -> {
                        send(counter);
                        notifySpace();
                    });
                }
            });
            log("COUNTER: [" + sessionId + "] RM" + currentTargetPrice
                    + termsText(counterTerms) + " [" + activeStrategyName + "]" + concessionText);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Notifies SpaceControl that the dealer completed an action this cycle. */
    private void notifySpace() {
        ACLMessage action = new ACLMessage(ACLMessage.INFORM);
        action.setOntology("ACTION_COMPLETED");
        action.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(action);
    }

    /** Returns true for broker messages that should wait while the simulation is paused. */
    private boolean isPausableMessage(String ontology) {
        return "BROKER_INVITE".equals(ontology) || "BROKER_RELAY_OFFER".equals(ontology);
    }

    /** Runs a delayed action immediately, or holds it until the simulation resumes. */
    private void runWhenResumed(Runnable action) {
        if (negotiationPaused) {
            pausedActions.add(action);
            return;
        }
        action.run();
    }

    /** Replays held delayed actions and inbound broker messages when the simulation resumes. */
    private void resumePausedWork() {
        while (!pausedActions.isEmpty() && !negotiationPaused) {
            pausedActions.remove(0).run();
        }
        while (!pausedMessages.isEmpty() && !negotiationPaused) {
            ACLMessage held = pausedMessages.remove(0);
            String ont = held.getOntology() == null ? "" : held.getOntology();
            if ("BROKER_INVITE".equals(ont) || "BROKER_RELAY_OFFER".equals(ont)) {
                handleBrokerOffer(held);
            }
        }
    }

    @Override
    /** Deregisters from SpaceControl when the dealer leaves the simulation. */
    protected void takeDown() {
        ACLMessage dereg = new ACLMessage(ACLMessage.INFORM);
        dereg.setOntology("DEREGISTER");
        dereg.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(dereg);
        log("Terminating");
    }

    /** Writes a dealer-prefixed message to the UI logger. */
    private void log(String m) {
        if (logger != null) logger.log(getLocalName() + ": " + m);
    }

    /** Describes the configured strategy switch for status logs. */
    private String strategySwitchText() {
        if (config.getStrategySwitchCycle() <= 0 || config.getSwitchStrategy() == config.getStrategy()) return "";
        return " → " + config.getSwitchStrategy() + " at cycle " + config.getStrategySwitchCycle();
    }

    /** Builds a counter-offer using the current target price and concession attributes. */
    private NegotiationTerms dealerCounterTerms() {
        double concession = (double) Math.max(0, retailPrice - currentTargetPrice)
                / Math.max(1, retailPrice - minPrice);
        int defaultWarranty = preferences.getDefaultWarrantyMonths();
        int warranty = Math.max(3, defaultWarranty - (int) Math.round(defaultWarranty * 0.5 * concession));
        int defaultDelivery = preferences.getDefaultDeliveryDays();
        int delivery = defaultDelivery + (int) Math.round(defaultDelivery * concession);
        return new NegotiationTerms(currentTargetPrice, warranty, delivery);
    }

    /** Calculates the target asking price for the current cycle or round. */
    private int calculateTargetPrice(int progressStep) {
        if (manualTargetPrice >= 0) {
            return Math.max(minPrice, manualTargetPrice);
        }
        int effectiveDeadline = effectiveDealerDeadlineCycles();
        int boundedStep = Math.min(Math.max(0, progressStep), effectiveDeadline);
        int strategyStep = scaleProgressToConfigCycle(boundedStep, effectiveDeadline);
        double concessionFactor = Math.pow((double) boundedStep / effectiveDeadline,
                config.betaForCycle(strategyStep));
        int target = (int) Math.round(retailPrice - ((retailPrice - minPrice) * concessionFactor));
        return Math.max(minPrice, (int) Math.round(target * config.getManualDealerTargetPercent()));
    }

    /** Advances the dealer target price when a negotiation round occurs. */
    private void moveTargetForNegotiationRound(int round) {
        if (manualTargetPrice >= 0) {
            currentTargetPrice = Math.max(minPrice, manualTargetPrice);
            return;
        }
        int progressStep = Math.max(latestCycle, round);
        int roundBasedTarget = calculateTargetPrice(progressStep);
        if (roundBasedTarget >= currentTargetPrice && currentTargetPrice > minPrice) {
            roundBasedTarget = currentTargetPrice - Math.max(1, (retailPrice - minPrice) / effectiveDealerDeadlineCycles());
        }
        currentTargetPrice = Math.max(minPrice, roundBasedTarget);
    }

    /** Extends the concession deadline for narrow retail-to-reserve price windows.
     *  ★ FIXED: Uses relative 30% of retail price threshold instead of hardcoded RM10,000.
     *  Deadline now triples (matching BuyerAgent behaviour) instead of doubling. */
    private int effectiveDealerDeadlineCycles() {
        int deadline = config.getDeadlineCycles();
        int priceWindow = retailPrice - minPrice;
        int narrowWindow = (int)(retailPrice * 0.30); // ★ FIXED: relative threshold
        if (priceWindow > 0 && priceWindow <= narrowWindow) {
            return Math.max(deadline, deadline * 3); // ★ FIXED: triple not double
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

    /** Returns true when an offer has enough dealer utility to accept. */
    private boolean acceptableUtility(NegotiationTerms terms) {
        double utility = preferences.dealerUtility(terms, retailPrice, minPrice,
                preferences.getDefaultWarrantyMonths() * 2,
                preferences.getDefaultDeliveryDays() * 2);
        return terms.getPrice() >= minPrice && utility >= 0.45;
    }

    /** Returns true when the first offer is too poor to continue negotiating. */
    private boolean isClearlyUnworkableFirstOffer(NegotiationTerms terms) {
        double utility = preferences.dealerUtility(terms, retailPrice, minPrice,
                preferences.getDefaultWarrantyMonths() * 2,
                preferences.getDefaultDeliveryDays() * 2);
        return terms.getPrice() < (int) (minPrice * 0.60) && utility < 0.05;
    }

    /** Formats non-price terms for compact status logging. */
    private String termsText(NegotiationTerms terms) {
        return " | Warranty=" + terms.getWarrantyMonths() + " months | Delivery="
                + terms.getDeliveryDays() + " days";
    }
}