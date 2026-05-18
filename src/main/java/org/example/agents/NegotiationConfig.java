package org.example.agents;

import java.io.Serializable;

// Carries strategy and timing settings that govern automated negotiation behavior.
public class NegotiationConfig implements Serializable {
    // Names the concession curve used to calculate offers over time.
    public enum Strategy {
        BOULWARE,
        CONCEDER,
        LINEAR
    }

    private final Strategy strategy;
    private final int deadlineCycles;
    private final double buyerStartPercent;
    private final double dealerReservePercent;
    private final int maxRoundsPerDealer;
    private final int maxSearchRetries;
    private final int stuckRoundsBeforeAcceleration;
    private final double manualDealerTargetPercent;
    private final int strategySwitchCycle;
    private final Strategy switchStrategy;

    // Creates a negotiation config without a mid-session strategy switch.
    public NegotiationConfig(
            Strategy strategy,
            int deadlineCycles,
            double buyerStartPercent,
            double dealerReservePercent,
            int maxRoundsPerDealer,
            int maxSearchRetries,
            int stuckRoundsBeforeAcceleration,
            double manualDealerTargetPercent
    ) {
        this(strategy, deadlineCycles, buyerStartPercent, dealerReservePercent, maxRoundsPerDealer,
                maxSearchRetries, stuckRoundsBeforeAcceleration, manualDealerTargetPercent, 0, strategy);
    }

    // Creates a full negotiation config, including optional strategy switching.
    public NegotiationConfig(
            Strategy strategy,
            int deadlineCycles,
            double buyerStartPercent,
            double dealerReservePercent,
            int maxRoundsPerDealer,
            int maxSearchRetries,
            int stuckRoundsBeforeAcceleration,
            double manualDealerTargetPercent,
            int strategySwitchCycle,
            Strategy switchStrategy
    ) {
        this.strategy = strategy;
        this.deadlineCycles = deadlineCycles;
        this.buyerStartPercent = buyerStartPercent;
        this.dealerReservePercent = dealerReservePercent;
        this.maxRoundsPerDealer = maxRoundsPerDealer;
        this.maxSearchRetries = maxSearchRetries;
        this.stuckRoundsBeforeAcceleration = stuckRoundsBeforeAcceleration;
        this.manualDealerTargetPercent = manualDealerTargetPercent;
        this.strategySwitchCycle = strategySwitchCycle;
        this.switchStrategy = switchStrategy;
    }

    // Returns the default negotiation config loaded from application properties.
    public static NegotiationConfig defaults() {
        return AppConfig.defaults().negotiationConfig();
    }

    // Returns the beta value for the initial strategy.
    public double beta() {
        return betaForStrategy(strategy);
    }

    // Returns the beta value after applying any cycle-based strategy switch.
    public double betaForCycle(int cycle) {
        return betaForStrategy(getEffectiveStrategy(cycle));
    }

    // Returns the active strategy for a cycle, switching when the threshold is reached.
    public Strategy getEffectiveStrategy(int cycle) {
        if (strategySwitchCycle > 0 && cycle >= strategySwitchCycle) {
            return switchStrategy;
        }
        return strategy;
    }

    // Maps a strategy name to the concession-curve exponent used by agents.
    private double betaForStrategy(Strategy activeStrategy) {
        switch (activeStrategy) {
            case CONCEDER:
                return 0.45;
            case LINEAR:
                return 1.0;
            case BOULWARE:
            default:
                return 2.0;
        }
    }

    // Returns the initial negotiation strategy.
    public Strategy getStrategy() {
        return strategy;
    }

    // Returns the cycle deadline used to normalize concession progress.
    public int getDeadlineCycles() {
        return deadlineCycles;
    }

    // Returns the buyer's first-offer percentage of budget.
    public double getBuyerStartPercent() {
        return buyerStartPercent;
    }

    // Returns the dealer reserve-price percentage of retail price.
    public double getDealerReservePercent() {
        return dealerReservePercent;
    }

    // Returns the maximum negotiation rounds allowed with one dealer.
    public int getMaxRoundsPerDealer() {
        return maxRoundsPerDealer;
    }

    // Returns how many broker search retries a buyer may make.
    public int getMaxSearchRetries() {
        return maxSearchRetries;
    }

    // Returns how many stagnant rounds trigger buyer concession acceleration.
    public int getStuckRoundsBeforeAcceleration() {
        return stuckRoundsBeforeAcceleration;
    }

    // Returns the manual-mode target price percentage for dealer acceptance.
    public double getManualDealerTargetPercent() {
        return manualDealerTargetPercent;
    }

    // Returns the cycle at which the strategy switches, or zero when disabled.
    public int getStrategySwitchCycle() {
        return strategySwitchCycle;
    }

    // Returns the strategy used after the switch cycle is reached.
    public Strategy getSwitchStrategy() {
        return switchStrategy;
    }
}
