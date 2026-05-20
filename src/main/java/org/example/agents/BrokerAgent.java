package org.example.agents;

import java.util.*;
import org.example.MainUI.UILogger;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * BrokerAgent — Phase 1
 *
 * Protocol contract: docs/protocol-contract.md
 *
 * Message ontologies handled (inbound):
 *   ""               — Dealer registration (INFORM, empty ontology)
 *   BUYER_SEARCH     — Buyer requests shortlist (REQUEST)
 *   BUYER_SHORTLIST  — Buyer selects dealer + first offer terms (PROPOSE) → creates session
 *   DEALER_COUNTER   — Dealer rejects with counter (REJECT_PROPOSAL) → relayed to buyer
 *   DEALER_ACCEPT    — Dealer accepts offer (ACCEPT_PROPOSAL) → settle session
 *   DEALER_REJECT    — Dealer declines buyer's first offer before negotiation
 *   BUYER_COUNTER    — Buyer counter-offer (PROPOSE) → relayed to dealer
 *   BUYER_WALKAWAY   — Buyer gives up on session (FAILURE) → close session
 *
 * Routes all buyer-dealer negotiation messages and records broker fees, sessions, and outcomes.
 */
public class BrokerAgent extends Agent {

    // ── Fee constants ──────────────────────────────────────────────────────────
    // ── State ──────────────────────────────────────────────────────────────────
    private UILogger logger;
    private final AppConfig appConfig = AppConfig.defaults();
    private final List<CarListing>              inventory    = new ArrayList<>();
    private final Map<String, NegotiationSession> sessions   = new LinkedHashMap<>();
    private final List<Transaction>             transactions = new ArrayList<>();

    private double totalRevenue    = 0;
    private int    noDealCount     = 0;
    private int    totalDealRounds = 0;

    // ── Inner types ────────────────────────────────────────────────────────────

    /** Registered dealer inventory entry used for shortlist search. */
    public static class CarListing {
        public String dealer, model;
        public int price, stock, reservePrice;
        /** Stores a dealer's model, list price, stock, and reserve price. */
        public CarListing(String d, String m, int p, int s, int r) {
            dealer = d; model = m; price = p; stock = s; reservePrice = r;
        }
    }

    /** Lifecycle states for a broker-routed negotiation session. */
    public enum SessionStatus { NEGOTIATING, SETTLED, FAILED, TIMEOUT }

    /** Active negotiation state tracked by the broker for one buyer-dealer pairing. */
    public static class NegotiationSession {
        public final String sessionId;
        public final String buyerId;
        public final String dealerId;
        public final String carModel;
        public NegotiationTerms currentTerms;
        public int    buyerReserve;
        public int    round;
        public SessionStatus status;
        public final long startTime;
        public boolean feeCharged;

        /** Creates a negotiating session from a buyer shortlist selection. */
        public NegotiationSession(String sid, String buyer, String dealer,
                                  String car, NegotiationTerms firstTerms, int reserve) {
            sessionId   = sid;
            buyerId     = buyer;
            dealerId    = dealer;
            carModel    = car;
            currentTerms = firstTerms;
            buyerReserve = reserve;
            round       = 0;
            status      = SessionStatus.NEGOTIATING;
            startTime   = System.currentTimeMillis();
            feeCharged  = false;
        }
    }

    /** Completed transaction record used for revenue and performance metrics. */
    public static class Transaction {
        public String buyer, dealer, car;
        public int price;
        public long timestamp;
        /** Stores the final deal parties, car, price, and settlement time. */
        public Transaction(String b, String d, String c, int p) {
            buyer = b; dealer = d; car = c; price = p;
            timestamp = System.currentTimeMillis();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    /** Initializes the broker, message router, and timeout scanner. */
    protected void setup() {
        if (getArguments() != null && getArguments().length > 0) {
            logger = (UILogger) getArguments()[0];
        }
        log("=== BROKER ONLINE ===");
        log(String.format("Fixed Negotiation Fee: RM%.0f | Commission: %.0f%% of sale price",
                appConfig.fixedFee(), appConfig.commissionRate() * 100));

        addBehaviour(new CyclicBehaviour() {
            /** Dispatches inbound ACL messages by ontology. */
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) { block(); return; }

                String ont = msg.getOntology() == null ? "" : msg.getOntology();
                switch (ont) {
                    case "":               handleDealerRegister(msg);  break;
                    case "BUYER_SEARCH":   handleBuyerSearch(msg);     break;
                    case "BUYER_SHORTLIST":handleBuyerShortlist(msg);  break;
                    case "DEALER_COUNTER": handleDealerCounter(msg);   break;
                    case "DEALER_ACCEPT":  handleDealerAccept(msg);    break;
                    case "DEALER_REJECT":  handleDealerReject(msg);    break;
                    case "DEALER_SOLD_OUT":handleDealerSoldOut(msg);   break;
                    case "BUYER_COUNTER":  handleBuyerCounter(msg);    break;
                    case "BUYER_WALKAWAY": handleBuyerWalkaway(msg);   break;
                    case "RESET_SESSION":  resetSessionState();        break;
                    default: break; // ignore REGISTER/DEREGISTER/CYCLE_UPDATE leaks
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, appConfig.timeoutScanMillis()) {
            /** Periodically closes sessions that exceeded the configured timeout. */
            @Override
            protected void onTick() {
                closeTimedOutSessions();
            }
        });
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    /** Dealer registration: INFORM / empty ontology / "car;price;stock;reserve" */
    private void handleDealerRegister(ACLMessage msg) {
        if (msg.getPerformative() != ACLMessage.INFORM) return;
        String[] d = msg.getContent().split(";");
        if (d.length < 2) return;
        int price   = Integer.parseInt(d[1]);
        int stock   = d.length > 2 ? Integer.parseInt(d[2]) : 1;
        int reserve = d.length > 3 ? Integer.parseInt(d[3]) : (int)(price * 0.70);
        inventory.add(new CarListing(msg.getSender().getLocalName(), d[0], price, stock, reserve));
        log("LISTING: " + d[0] + " @ RM" + price + " | Reserve: RM" + reserve
                + " | Stock: " + stock + " (Seller: " + msg.getSender().getLocalName() + ")");
    }

    /** Buyer search: REQUEST / BUYER_SEARCH / "sessionId;carModel" */
    private void handleBuyerSearch(ACLMessage msg) {
        String[] parts = msg.getContent().split(";", 2);
        String sessionId = parts[0];
        String carModel  = parts.length > 1 ? parts[1] : parts[0];

        StringBuilder results = new StringBuilder();
        int matchCount = 0;
        for (CarListing cl : inventory) {
            if (cl.model.equalsIgnoreCase(carModel) && cl.stock > 0) {
                results.append(cl.dealer).append(":").append(cl.price)
                       .append(":").append(cl.reservePrice).append(",");
                matchCount++;
            }
        }
        ACLMessage reply = msg.createReply();
        reply.setPerformative(ACLMessage.PROPOSE);
        reply.setOntology("BROKER_SHORTLIST");
        reply.setContent(sessionId + ";" + (results.length() > 0 ? results.toString() : "NONE"));
        send(reply);

        log(matchCount > 0
                ? "SEARCH: Found " + matchCount + " " + carModel + "(s) for " + msg.getSender().getLocalName()
                : "SEARCH: No " + carModel + " available for " + msg.getSender().getLocalName());
    }

    /**
     * Buyer shortlist submission: PROPOSE / BUYER_SHORTLIST
     * Content: "sessionId;dealerName;terms;buyerReserve;carModel"
     * → Creates session, charges fixed fee, invites dealer.
     */
    private void handleBuyerShortlist(ACLMessage msg) {
        String[] p = msg.getContent().split(";");
        if (p.length < 5) {
            log("ERROR: Malformed BUYER_SHORTLIST: " + msg.getContent());
            return;
        }
        String sessionId  = p[0];
        String dealerName = p[1];
        NegotiationTerms firstTerms = NegotiationTerms.fromPayload(p[2]);
        int buyerReserve  = Integer.parseInt(p[3]);
        String carModel   = p[4];
        String buyerName  = msg.getSender().getLocalName();

        NegotiationSession existing = sessions.get(sessionId);
        if (existing != null && existing.status == SessionStatus.NEGOTIATING) {
            log("ERROR: Duplicate active session rejected: " + sessionId);
            ACLMessage reject = new ACLMessage(ACLMessage.FAILURE);
            reject.addReceiver(new AID(buyerName, AID.ISLOCALNAME));
            reject.setOntology("BROKER_SESSION_REJECTED");
            reject.setContent(sessionId + ";DUPLICATE_SESSION");
            send(reject);
            return;
        }

        NegotiationSession session = new NegotiationSession(
                sessionId, buyerName, dealerName, carModel, firstTerms, buyerReserve);
        sessions.put(sessionId, session);

        int dealerReserve = lookupDealerReserve(dealerName, carModel);

        // Charge fixed fee at session start
        totalRevenue += appConfig.fixedFee();
        session.feeCharged = true;
        log("SESSION START: " + sessionId + " | Buyer=" + buyerName
            + " | Dealer=" + dealerName + " | Car=" + carModel + " | FirstOffer=RM" + firstTerms.getPrice()
            + " | Warranty=" + firstTerms.getWarrantyMonths() + " months | Delivery=" + firstTerms.getDeliveryDays() + " days"
            + " | BuyerReserve=RM" + buyerReserve
            + (dealerReserve > 0 ? " | DealerReserve=RM" + dealerReserve : ""));
        log("FEE CHARGED: RM" + (int) appConfig.fixedFee() + " | Running Revenue: RM" + (int) totalRevenue);

        // Invite dealer with buyer's first offer
        ACLMessage invite = new ACLMessage(ACLMessage.REQUEST);
        invite.addReceiver(new AID(dealerName, AID.ISLOCALNAME));
        invite.setOntology("BROKER_INVITE");
        invite.setContent(sessionId + ";" + buyerName + ";" + carModel + ";" + firstTerms.toPayload());
        send(invite);
        log("INVITE: Sent RM" + firstTerms.getPrice() + " offer to " + dealerName
                + " | Warranty=" + firstTerms.getWarrantyMonths() + " months | Delivery="
                + firstTerms.getDeliveryDays() + " days");
    }

    /**
     * Dealer counter: REJECT_PROPOSAL / DEALER_COUNTER / "sessionId;counterTerms"
     * → Relay counter to buyer.
     */
    private void handleDealerCounter(ACLMessage msg) {
        String[] p = msg.getContent().split(";", 2);
        String sessionId = p[0];
        NegotiationTerms counter = NegotiationTerms.fromPayload(p[1]);
        NegotiationSession s = sessions.get(sessionId);
        if (s == null) { log("ERROR: Unknown session in DEALER_COUNTER: " + sessionId); return; }
        if (s.status != SessionStatus.NEGOTIATING) {
            log("ERROR: Ignored DEALER_COUNTER for closed session: " + sessionId);
            return;
        }

        s.round++;
        s.currentTerms = counter;
        log("RELAY COUNTER: " + sessionId + " | Dealer=" + s.dealerId
                + " → Buyer=" + s.buyerId + " | RM" + counter.getPrice()
                + " | Warranty=" + counter.getWarrantyMonths() + " months | Delivery="
                + counter.getDeliveryDays() + " days (Round " + s.round + ")");

        ACLMessage relay = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
        relay.addReceiver(new AID(s.buyerId, AID.ISLOCALNAME));
        relay.setOntology("BROKER_RELAY_COUNTER");
        relay.setContent(sessionId + ";" + s.dealerId + ";" + counter.toPayload());
        send(relay);
    }

    /**
     * Dealer accept: ACCEPT_PROPOSAL / DEALER_ACCEPT / "sessionId;agreedTerms"
     * → Charge commission, settle session, notify buyer.
     */
    private void handleDealerAccept(ACLMessage msg) {
        String[] p    = msg.getContent().split(";", 2);
        String sessionId  = p[0];
        NegotiationTerms agreedTerms = NegotiationTerms.fromPayload(p[1]);
        int agreedPrice = agreedTerms.getPrice();
        NegotiationSession s = sessions.get(sessionId);
        if (s == null) { log("ERROR: Unknown session in DEALER_ACCEPT: " + sessionId); return; }
        if (s.status != SessionStatus.NEGOTIATING) {
            log("ERROR: Ignored DEALER_ACCEPT for closed session: " + sessionId);
            return;
        }

        s.round++;
        s.status = SessionStatus.SETTLED;
        s.currentTerms = agreedTerms;
        double commission = agreedPrice * appConfig.commissionRate();
        totalRevenue   += commission;
        totalDealRounds += s.round;
        reduceStock(s.dealerId, s.carModel);
        transactions.add(new Transaction(s.buyerId, s.dealerId, s.carModel, agreedPrice));

        log("DEAL SETTLED: " + sessionId + " | Buyer=" + s.buyerId + " | Dealer=" + s.dealerId
                + " | Car=" + s.carModel + " | Price=RM" + agreedPrice
                + " | Warranty=" + agreedTerms.getWarrantyMonths() + " months | Delivery="
                + agreedTerms.getDeliveryDays() + " days"
                + " | Commission=RM" + (int) commission + " | Fee=RM" + (int) appConfig.fixedFee() + " (charged at start)");
        log("REVENUE: +RM" + (int) commission + " commission | Total: RM" + (int) totalRevenue);
        log("TOTAL TRANSACTIONS: " + transactions.size());
        logPerformanceMetrics();

        // Notify buyer
        ACLMessage notify = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        notify.addReceiver(new AID(s.buyerId, AID.ISLOCALNAME));
        notify.setOntology("BROKER_RELAY_ACCEPT");
        notify.setContent(sessionId + ";" + s.dealerId + ";" + agreedTerms.toPayload());
        send(notify);
    }

    /**
     * Buyer counter: PROPOSE / BUYER_COUNTER / "sessionId;newTerms"
     * → Relay new offer to dealer.
     */
    private void handleBuyerCounter(ACLMessage msg) {
        String[] p = msg.getContent().split(";", 2);
        String sessionId = p[0];
        NegotiationTerms newTerms = NegotiationTerms.fromPayload(p[1]);
        NegotiationSession s = sessions.get(sessionId);
        if (s == null) { log("ERROR: Unknown session in BUYER_COUNTER: " + sessionId); return; }
        if (s.status != SessionStatus.NEGOTIATING) {
            log("ERROR: Ignored BUYER_COUNTER for closed session: " + sessionId);
            return;
        }

        s.currentTerms = newTerms;
        log("RELAY OFFER: " + sessionId + " | Buyer=" + s.buyerId
                + " → Dealer=" + s.dealerId + " | RM" + newTerms.getPrice()
                + " | Warranty=" + newTerms.getWarrantyMonths() + " months | Delivery="
                + newTerms.getDeliveryDays() + " days");

        ACLMessage relay = new ACLMessage(ACLMessage.PROPOSE);
        relay.addReceiver(new AID(s.dealerId, AID.ISLOCALNAME));
        relay.setOntology("BROKER_RELAY_OFFER");
        relay.setContent(sessionId + ";" + s.buyerId + ";" + s.carModel + ";" + newTerms.toPayload());
        send(relay);
    }

    /** Dealer rejection: REJECT_PROPOSAL / DEALER_REJECT closes a failed first-offer session. */
    private void handleDealerReject(ACLMessage msg) {
        String[] p = msg.getContent().split(";", 2);
        String sessionId = p[0];
        String reason = p.length > 1 ? p[1] : "DEALER_REJECTED_OFFER";
        NegotiationSession s = sessions.get(sessionId);
        if (s == null || s.status != SessionStatus.NEGOTIATING) {
            return;
        }
        s.status = SessionStatus.FAILED;
        noDealCount++;
        log("NO DEAL: " + sessionId + " | Reason=" + reason + " | Buyer=" + s.buyerId
                + " | Dealer=" + s.dealerId + " | Car=" + s.carModel);
        logPerformanceMetrics();

        ACLMessage notify = new ACLMessage(ACLMessage.FAILURE);
        notify.addReceiver(new AID(s.buyerId, AID.ISLOCALNAME));
        notify.setOntology("BROKER_SESSION_REJECTED");
        notify.setContent(sessionId + ";" + reason);
        send(notify);
    }

    /**
     * Buyer walkaway: FAILURE / BUYER_WALKAWAY / "sessionId;reason"
     * → Mark session FAILED. Fixed fee already collected.
     */
    private void handleBuyerWalkaway(ACLMessage msg) {
        String[] p = msg.getContent().split(";");
        String sessionId = p[0];
        String reason    = p.length > 1 ? p[1] : "UNKNOWN";
        NegotiationSession s = sessions.get(sessionId);
        if (s != null) {
            if (s.status != SessionStatus.NEGOTIATING) {
                log("ERROR: Ignored duplicate no-deal for closed session: " + sessionId);
                return;
            }
            s.status = SessionStatus.FAILED;
            noDealCount++;
            log("NO DEAL: " + sessionId + " | Reason=" + reason
                    + " | Buyer=" + s.buyerId + " | Car=" + s.carModel);
            logPerformanceMetrics();
        } else {
            String car = p.length > 2 ? p[2] : "UNKNOWN";
            String budget = p.length > 3 ? p[3] : "UNKNOWN";
            noDealCount++;
            log("NO DEAL: " + sessionId + " | Reason=" + reason
                    + " | Buyer=" + msg.getSender().getLocalName()
                    + " | Car=" + car + " | Budget=RM" + budget
                    + " | No session fee charged");
            logPerformanceMetrics();
        }
    }

    /**
     * Dealer sold out: INFORM / DEALER_SOLD_OUT / "sessionId1,sessionId2,..."
     * Dealer went out of stock mid-negotiation. Close every listed session and
     * notify the affected buyers so they can immediately try the next dealer.
     */
    private void handleDealerSoldOut(ACLMessage msg) {
        String dealerName = msg.getSender().getLocalName();
        String[] sessionIds = msg.getContent().split(",");
        for (String sessionId : sessionIds) {
            sessionId = sessionId.trim();
            if (sessionId.isEmpty()) continue;
            NegotiationSession s = sessions.get(sessionId);
            if (s == null) continue;
            if (s.status != SessionStatus.NEGOTIATING) continue; // already closed

            s.status = SessionStatus.FAILED;
            noDealCount++;
            log("NO DEAL: " + sessionId + " | Reason=DEALER_SOLD_OUT | Buyer=" + s.buyerId
                    + " | Dealer=" + dealerName + " | Car=" + s.carModel);
            logPerformanceMetrics();

            // Notify buyer so they advance to the next dealer without waiting
            ACLMessage notify = new ACLMessage(ACLMessage.INFORM);
            notify.addReceiver(new AID(s.buyerId, AID.ISLOCALNAME));
            notify.setOntology("BROKER_RELAY_SOLD_OUT");
            notify.setContent(sessionId + ";" + dealerName);
            send(notify);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Decrements stock for a successful deal and removes sold-out listings. */
    private void reduceStock(String dealerName, String carModel) {
        Iterator<CarListing> iterator = inventory.iterator();
        while (iterator.hasNext()) {
            CarListing cl = iterator.next();
            if (cl.dealer.equals(dealerName)
                    && cl.model.equalsIgnoreCase(carModel)
                    && cl.stock > 0) {
                cl.stock--;
                if (cl.stock == 0) {
                    iterator.remove();
                    log("LISTING REMOVED: " + carModel + " from " + dealerName + " | Reason=SOLD_OUT");
                }
                return;
            }
        }
    }

    /** Looks up the dealer reserve price for a listed car. */
    private int lookupDealerReserve(String dealerName, String carModel) {
        for (CarListing cl : inventory) {
            if (cl.dealer.equals(dealerName) && cl.model.equalsIgnoreCase(carModel)) {
                return cl.reservePrice;
            }
        }
        return -1;
    }

    /** Marks stale negotiating sessions as timed out and notifies affected buyers. */
    private void closeTimedOutSessions() {
        long now = System.currentTimeMillis();
        for (NegotiationSession s : sessions.values()) {
            if (s.status != SessionStatus.NEGOTIATING) {
                continue;
            }
            if (now - s.startTime < appConfig.sessionTimeoutMillis()) {
                continue;
            }

            s.status = SessionStatus.TIMEOUT;
            noDealCount++;
            log("NO DEAL: " + s.sessionId + " | Reason=TIMEOUT | Buyer=" + s.buyerId
                    + " | Dealer=" + s.dealerId + " | Car=" + s.carModel);
            logPerformanceMetrics();

            ACLMessage notify = new ACLMessage(ACLMessage.FAILURE);
            notify.addReceiver(new AID(s.buyerId, AID.ISLOCALNAME));
            notify.setOntology("BROKER_SESSION_REJECTED");
            notify.setContent(s.sessionId + ";TIMEOUT");
            send(notify);
        }
    }

    /** Logs aggregate deal, no-deal, price, round, and success metrics. */
    private void logPerformanceMetrics() {
        int total = transactions.size() + noDealCount;
        double avgPrice  = transactions.stream().mapToInt(t -> t.price).average().orElse(0);
        double avgRounds = transactions.isEmpty() ? 0 : (double) totalDealRounds / transactions.size();
        double successRate = total == 0 ? 0 : (transactions.size() * 100.0) / total;
        log(String.format(
                "PERFORMANCE: Deals=%d | NoDeals=%d | AvgDeal=RM%.0f | AvgRounds=%.1f | SuccessRate=%.1f%%",
                transactions.size(), noDealCount, avgPrice, avgRounds, successRate));
    }

    /** Clears all broker state for a new demo run. */
    private void resetSessionState() {
        inventory.clear();
        sessions.clear();
        transactions.clear();
        totalRevenue = 0;
        noDealCount = 0;
        totalDealRounds = 0;
        log("RESET: Broker inventory, sessions, revenue, and metrics cleared.");
    }

    /** Writes a broker-prefixed message to the UI logger. */
    private void log(String m) {
        if (logger != null) logger.log("[BROKER] " + m);
    }

    // ── Public accessors (for GUI dashboard, Phase 4) ─────────────────────────

    /** Returns a read-only view of current inventory listings. */
    public List<CarListing>                    getInventory()    { return Collections.unmodifiableList(inventory); }
    /** Returns a read-only view of broker sessions. */
    public Map<String, NegotiationSession>     getSessions()     { return Collections.unmodifiableMap(sessions); }
    /** Returns a read-only view of completed transactions. */
    public List<Transaction>                   getTransactions() { return Collections.unmodifiableList(transactions); }
    /** Returns total broker revenue from fees and commissions. */
    public double getTotalRevenue()   { return totalRevenue; }
    /** Returns total failed or no-deal session count. */
    public int    getNoDealCount()    { return noDealCount; }
}
