package org.example.agents;

import java.util.*;
import org.example.MainUI.UILogger;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

// Routes every negotiation message between buyers and dealers, owns sessions, fees, and market records.
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

    // Stores one dealer's available inventory entry and reserve price.
    public static class CarListing {
        public String dealer, model;
        public int price, stock, reservePrice;
        // Creates an inventory listing published by a dealer agent.
        public CarListing(String d, String m, int p, int s, int r) {
            dealer = d; model = m; price = p; stock = s; reservePrice = r;
        }
    }

    // Tracks the lifecycle state of a broker-managed negotiation session.
    public enum SessionStatus { NEGOTIATING, SETTLED, FAILED, TIMEOUT }

    // Holds per-session state so concurrent negotiations stay isolated by sessionId.
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

        // Creates a new active session from the buyer's first broker-routed offer.
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

    // Records one completed sale for dashboard metrics.
    public static class Transaction {
        public String buyer, dealer, car;
        public int price;
        public long timestamp;
        // Creates a transaction record with the current completion timestamp.
        public Transaction(String b, String d, String c, int p) {
            buyer = b; dealer = d; car = c; price = p;
            timestamp = System.currentTimeMillis();
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    // Initializes broker logging, message routing, and periodic session timeout scanning.
    protected void setup() {
        if (getArguments() != null && getArguments().length > 0) {
            logger = (UILogger) getArguments()[0];
        }
        log("=== BROKER ONLINE ===");
        log(String.format("Fixed Negotiation Fee: RM%.0f | Commission: %.0f%% of sale price",
                appConfig.fixedFee(), appConfig.commissionRate() * 100));

        addBehaviour(new CyclicBehaviour() {
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
                    default: break; // ignore REGISTER/DEREGISTER/CYCLE_UPDATE leaks
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, appConfig.timeoutScanMillis()) {
            @Override
            protected void onTick() {
                closeTimedOutSessions();
            }
        });
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    // Handles dealer registration: INFORM / empty ontology / "car;price;stock;reserve".
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

    // Handles buyer search: REQUEST / BUYER_SEARCH / "sessionId;carModel".
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

    // Creates a session from BUYER_SHORTLIST and invites the selected dealer.
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

    // Relays DEALER_COUNTER terms from the dealer to the buyer for this session.
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

    // Settles DEALER_ACCEPT, records revenue and transaction data, and notifies the buyer.
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

    // Relays BUYER_COUNTER terms from the buyer to the dealer for this session.
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

    // Handles DEALER_REJECT by failing the session and notifying the buyer.
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

    // Handles BUYER_WALKAWAY by failing an active session or recording a pre-session no-deal.
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

    // Handles DEALER_SOLD_OUT by failing affected sessions and telling buyers to continue.
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

    // Decrements stock for the dealer and model involved in a settled transaction.
    private void reduceStock(String dealerName, String carModel) {
        for (CarListing cl : inventory) {
            if (cl.dealer.equals(dealerName)
                    && cl.model.equalsIgnoreCase(carModel)
                    && cl.stock > 0) {
                cl.stock--;
                return;
            }
        }
    }

    // Finds the reserve price for a dealer/model listing, or -1 when it is unknown.
    private int lookupDealerReserve(String dealerName, String carModel) {
        for (CarListing cl : inventory) {
            if (cl.dealer.equals(dealerName) && cl.model.equalsIgnoreCase(carModel)) {
                return cl.reservePrice;
            }
        }
        return -1;
    }

    // Fails active sessions that exceed the configured timeout and informs buyers.
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

    // Logs aggregate broker performance metrics after a session outcome changes.
    private void logPerformanceMetrics() {
        int total = transactions.size() + noDealCount;
        double avgPrice  = transactions.stream().mapToInt(t -> t.price).average().orElse(0);
        double avgRounds = transactions.isEmpty() ? 0 : (double) totalDealRounds / transactions.size();
        double successRate = total == 0 ? 0 : (transactions.size() * 100.0) / total;
        log(String.format(
                "PERFORMANCE: Deals=%d | NoDeals=%d | AvgDeal=RM%.0f | AvgRounds=%.1f | SuccessRate=%.1f%%",
                transactions.size(), noDealCount, avgPrice, avgRounds, successRate));
    }

    // Sends a broker-prefixed message to the UI logger when one is configured.
    private void log(String m) {
        if (logger != null) logger.log("[BROKER] " + m);
    }

    // ── Public accessors (for GUI dashboard, Phase 4) ─────────────────────────

    // Returns a read-only view of current broker inventory for the dashboard.
    public List<CarListing>                    getInventory()    { return Collections.unmodifiableList(inventory); }
    // Returns a read-only view of broker sessions for the dashboard.
    public Map<String, NegotiationSession>     getSessions()     { return Collections.unmodifiableMap(sessions); }
    // Returns a read-only view of completed transactions for the dashboard.
    public List<Transaction>                   getTransactions() { return Collections.unmodifiableList(transactions); }
    // Returns total fixed-fee and commission revenue collected by the broker.
    public double getTotalRevenue()   { return totalRevenue; }
    // Returns the number of failed, timed out, or pre-session no-deal outcomes.
    public int    getNoDealCount()    { return noDealCount; }
}
