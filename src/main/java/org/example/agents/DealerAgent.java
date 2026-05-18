package org.example.agents;

import jade.core.*;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import org.example.MainUI.UILogger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// Represents a seller that negotiates through the broker and adjusts target price over market cycles.
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
    private NegotiationConfig.Strategy activeStrategy;
    private final Map<String, DealerSessionState> activeSessions = new LinkedHashMap<>();

    // Stores one buyer session so simultaneous broker-routed offers do not overwrite each other.
    private static class DealerSessionState {
        String sessionId;
        String buyerName;
        String carModel;
        NegotiationTerms latestTerms;
        int rounds;
        String status;

        // Creates per-session state from the first broker-relayed offer.
        DealerSessionState(String sessionId, String buyerName, String carModel, NegotiationTerms latestTerms) {
            this.sessionId = sessionId;
            this.buyerName = buyerName;
            this.carModel = carModel;
            this.latestTerms = latestTerms;
            this.status = "NEGOTIATING";
        }
    }

    @Override
    // Initializes listing details, registers with broker and SpaceControl, and starts message handling.
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
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                String ont = msg.getOntology() == null ? "" : msg.getOntology();

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

    // Handles CYCLE_UPDATE or START_CYCLE by recalculating the dealer's target price.
    private void handleCycleUpdate(ACLMessage msg) {
        int currentCycle = Integer.parseInt(msg.getContent());
        int t = Math.min(currentCycle, config.getDeadlineCycles());

        NegotiationConfig.Strategy effective = config.getEffectiveStrategy(t);
        if (effective != activeStrategy) {
            activeStrategy = effective;
            log("STATUS: Strategy shifted to " + activeStrategy + " at cycle " + t);
        }
        double concessionFactor = Math.pow((double) t / config.getDeadlineCycles(), config.betaForCycle(t));
        int cycleTarget = (int)(retailPrice - ((retailPrice - minPrice) * concessionFactor));
        currentTargetPrice = manualTargetPrice >= 0
                ? Math.max(minPrice, manualTargetPrice)
                : Math.max(minPrice, (int)(cycleTarget * config.getManualDealerTargetPercent()));
        log("Price updated to RM" + currentTargetPrice + " (cycle " + t + ")");
    }

    // Handles PRICE_ADJUSTMENT from the UI by overriding the target price floor.
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

    // Handles BROKER_INVITE and BROKER_RELAY_OFFER using "sessionId;buyerName;carModel;terms".
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

        log("OFFER #" + negotiationCount + ": [" + sessionId + "] Buyer offered RM" + buyerOffer
                + termsText(buyerTerms) + " (target=RM" + currentTargetPrice + ")");

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
                @Override
                protected void onWake() {
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
                }
            });
            log("DEAL CLOSED: [" + sessionId + "] RM" + buyerOffer + termsText(buyerTerms)
                    + " | Stock: " + stockCount);
        } else {
            state.status = "COUNTERED";
            NegotiationTerms counterTerms = dealerCounterTerms();
            // Counter-offer
            ACLMessage counter = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            counter.addReceiver(new AID("broker", AID.ISLOCALNAME));
            counter.setOntology("DEALER_COUNTER");
            counter.setContent(sessionId + ";" + counterTerms.toPayload());
            
            addBehaviour(new jade.core.behaviours.WakerBehaviour(this, 600) {
                @Override
                protected void onWake() {
                    send(counter);
                    notifySpace();
                }
            });
            log("COUNTER: [" + sessionId + "] RM" + currentTargetPrice + termsText(counterTerms));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Notifies SpaceControl that this dealer has completed its current market action.
    private void notifySpace() {
        ACLMessage action = new ACLMessage(ACLMessage.INFORM);
        action.setOntology("ACTION_COMPLETED");
        action.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(action);
    }

    @Override
    // Deregisters from SpaceControl when the dealer agent terminates.
    protected void takeDown() {
        ACLMessage dereg = new ACLMessage(ACLMessage.INFORM);
        dereg.setOntology("DEREGISTER");
        dereg.addReceiver(new AID("space", AID.ISLOCALNAME));
        send(dereg);
        log("Terminating");
    }

    // Sends a dealer-prefixed message to the UI logger when one is configured.
    private void log(String m) {
        if (logger != null) logger.log(getLocalName() + ": " + m);
    }

    // Formats the configured strategy switch for startup status messages.
    private String strategySwitchText() {
        if (config.getStrategySwitchCycle() <= 0 || config.getSwitchStrategy() == config.getStrategy()) return "";
        return " → " + config.getSwitchStrategy() + " at cycle " + config.getStrategySwitchCycle();
    }

    // Builds the dealer's current counter-offer terms from target price and concession progress.
    private NegotiationTerms dealerCounterTerms() {
        double concession = (double) Math.max(0, retailPrice - currentTargetPrice)
                / Math.max(1, retailPrice - minPrice);
        int defaultWarranty = preferences.getDefaultWarrantyMonths();
        int warranty = Math.max(3, defaultWarranty - (int) Math.round(defaultWarranty * 0.5 * concession));
        int defaultDelivery = preferences.getDefaultDeliveryDays();
        int delivery = defaultDelivery + (int) Math.round(defaultDelivery * concession);
        return new NegotiationTerms(currentTargetPrice, warranty, delivery);
    }

    // Returns whether incoming terms meet the dealer's reserve and utility threshold.
    private boolean acceptableUtility(NegotiationTerms terms) {
        double utility = preferences.dealerUtility(terms, retailPrice, minPrice,
                preferences.getDefaultWarrantyMonths() * 2,
                preferences.getDefaultDeliveryDays() * 2);
        return terms.getPrice() >= minPrice && utility >= 0.45;
    }

    // Identifies first offers too far below reserve and utility to enter negotiation.
    private boolean isClearlyUnworkableFirstOffer(NegotiationTerms terms) {
        double utility = preferences.dealerUtility(terms, retailPrice, minPrice,
                preferences.getDefaultWarrantyMonths() * 2,
                preferences.getDefaultDeliveryDays() * 2);
        return terms.getPrice() < (int) (minPrice * 0.60) && utility < 0.05;
    }

    // Formats non-price terms for dealer log messages.
    private String termsText(NegotiationTerms terms) {
        return " | Warranty=" + terms.getWarrantyMonths() + " months | Delivery="
                + terms.getDeliveryDays() + " days";
    }
}
