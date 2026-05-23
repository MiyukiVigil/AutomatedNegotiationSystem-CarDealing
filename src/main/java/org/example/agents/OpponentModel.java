package org.example.agents;
import java.util.ArrayList;
import java.util.List;

/**
 * OpponentModel — Tracks offer history from the opposing agent and predicts
 * their next offer and the round at which they will reach a target price.
 *
 * Used by BuyerAgent to model the dealer's concession behaviour, and by
 * DealerAgent to model the buyer's willingness to pay over time.
 *
 * Algorithm: Linear regression over the last N observed offers to estimate
 * the average change per round, then extrapolates forward.
 *
 * Example (Buyer modelling Dealer):
 *   Round 1: Dealer offers RM84000
 *   Round 2: Dealer offers RM81000
 *   Round 3: Dealer offers RM78000
 *   Avg change: -RM3000/round
 *   Predicted next: RM75000
 *   Rounds to reach RM70000: ~3 more rounds
 */
public class OpponentModel {

    /** Maximum number of recent offers used for trend calculation. */
    private static final int WINDOW = 5;

    /** Label used in log messages to identify which side is being modelled. */
    private final String label;

    /** Chronological list of observed offer prices from the opponent. */
    private final List<Integer> offerHistory = new ArrayList<>();

    /** Creates a model for the named opponent role (e.g. "dealer", "buyer"). */
    public OpponentModel(String label) {
        this.label = label;
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    /** Records a new observed offer price from the opponent. */
    public void recordOffer(int price) {
        offerHistory.add(price);
    }

    /** Returns how many offers have been recorded so far. */
    public int size() {
        return offerHistory.size();
    }

    /** Returns the most recent observed offer, or -1 if no data yet. */
    public int lastOffer() {
        if (offerHistory.isEmpty()) return -1;
        return offerHistory.get(offerHistory.size() - 1);
    }

    // ── Prediction ────────────────────────────────────────────────────────────

    /**
     * Predicts the opponent's next offer based on recent trend.
     * Returns -1 if there is not enough data (fewer than 2 offers).
     */
    public int predictNextOffer() {
        if (offerHistory.size() < 2) return -1;
        double avgChange = averageChangePerRound();
        int last = lastOffer();
        return (int) Math.round(last + avgChange);
    }

    /**
     * Predicts how many more rounds until the opponent reaches the target price.
     * Returns -1 if prediction is not possible (not enough data or no movement).
     *
     * @param targetPrice the price we want the opponent to reach
     */
    public int roundsToReachTarget(int targetPrice) {
        if (offerHistory.size() < 2) return -1;
        double avgChange = averageChangePerRound();
        if (Math.abs(avgChange) < 1) return -1; // opponent is not moving
        int last = lastOffer();
        double rounds = (targetPrice - last) / avgChange;
        if (rounds < 0) return 0; // already past target
        return (int) Math.ceil(rounds);
    }

    /**
     * Returns true if the opponent is predicted to reach the target price
     * within the given number of rounds.
     *
     * @param targetPrice  the price threshold to check
     * @param withinRounds how many rounds to look ahead
     */
    public boolean willReachTarget(int targetPrice, int withinRounds) {
        int rounds = roundsToReachTarget(targetPrice);
        return rounds >= 0 && rounds <= withinRounds;
    }

    /**
     * Returns a human-readable summary of the current prediction.
     * Used for logging inside agents.
     */
    public String summary(int myCurrentOffer) {
        if (offerHistory.size() < 2) {
            return "[" + label + " model] Not enough data yet (" + offerHistory.size() + " offers recorded)";
        }
        int predicted = predictNextOffer();
        double avg = averageChangePerRound();
        int rounds = roundsToReachTarget(myCurrentOffer);
        String direction = avg < 0 ? "conceding ▼" : avg > 0 ? "rising ▲" : "flat →";
        return String.format(
                "[%s model] Last=RM%d | AvgChange=RM%.0f/round (%s) | PredictedNext=RM%d | RoundsToMeetMe=%s",
                label, lastOffer(), avg, direction, predicted,
                rounds < 0 ? "unknown" : String.valueOf(rounds)
        );
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * Calculates the average change per round over the recent window of offers.
     * Uses the last WINDOW offers to avoid being skewed by early outliers.
     */
    private double averageChangePerRound() {
        int size = offerHistory.size();
        int windowStart = Math.max(0, size - WINDOW);
        List<Integer> window = offerHistory.subList(windowStart, size);
        if (window.size() < 2) return 0;
        int totalChange = 0;
        for (int i = 1; i < window.size(); i++) {
            totalChange += window.get(i) - window.get(i - 1);
        }
        return (double) totalChange / (window.size() - 1);
    }
}