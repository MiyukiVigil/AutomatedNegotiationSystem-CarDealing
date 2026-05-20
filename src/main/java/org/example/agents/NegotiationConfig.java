package org.example.agents;

import java.io.Serializable;

/** Immutable timing and concession settings shared by buyers, dealers, and the UI. */
public class NegotiationConfig implements Serializable {
    /** Supported concession curve profiles. */
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

    /** Creates a configuration without a mid-run strategy switch. */
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

    /** Creates a configuration with optional strategy switching after a cycle threshold. */
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

    /** Returns the configured default negotiation settings. */
    public static NegotiationConfig defaults() {
        return AppConfig.defaults().negotiationConfig();
    }

    /** Returns the beta value for the initial strategy. */
    public double beta() {
        return betaForStrategy(strategy);
    }

    /** Returns the beta value for the effective strategy at a cycle. */
    public double betaForCycle(int cycle) {
        return betaForStrategy(getEffectiveStrategy(cycle));
    }

    /** Returns the active strategy after applying the switch-cycle rule. */
    public Strategy getEffectiveStrategy(int cycle) {
        if (strategySwitchCycle > 0 && cycle >= strategySwitchCycle) {
            return switchStrategy;
        }
        return strategy;
    }

    /** Maps a strategy profile to its concession curve beta value. */
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

    /** Returns the initial concession strategy. */
    public Strategy getStrategy() {
        return strategy;
    }

    /** Returns the negotiation deadline in simulation cycles. */
    public int getDeadlineCycles() {
        return deadlineCycles;
    }

    /** Returns the buyer starting offer as a percent of budget. */
    public double getBuyerStartPercent() {
        return buyerStartPercent;
    }

    /** Returns the dealer reserve as a percent of retail price. */
    public double getDealerReservePercent() {
        return dealerReservePercent;
    }

    /** Returns the maximum negotiation rounds per dealer. */
    public int getMaxRoundsPerDealer() {
        return maxRoundsPerDealer;
    }

    /** Returns how many times a buyer retries market search. */
    public int getMaxSearchRetries() {
        return maxSearchRetries;
    }

    /** Returns how many flat rounds trigger acceleration. */
    public int getStuckRoundsBeforeAcceleration() {
        return stuckRoundsBeforeAcceleration;
    }

    /** Returns the manual dealer target as a percent of retail price. */
    public double getManualDealerTargetPercent() {
        return manualDealerTargetPercent;
    }

    /** Returns the cycle at which strategy switching begins. */
    public int getStrategySwitchCycle() {
        return strategySwitchCycle;
    }

    /** Returns the strategy used after the switch cycle. */
    public Strategy getSwitchStrategy() {
        return switchStrategy;
    }
}
