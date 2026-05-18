package org.example.agents;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Loads and exposes configurable defaults for broker fees, strategy settings, and utility weights.
public final class AppConfig {
    private static final String RESOURCE_NAME = "negotiation-defaults.properties";
    private static final AppConfig INSTANCE = load();

    private final Properties values;

    // Keeps the loaded property set behind the singleton accessor.
    private AppConfig(Properties values) {
        this.values = values;
    }

    // Returns the shared application defaults instance.
    public static AppConfig defaults() {
        return INSTANCE;
    }

    // Reads the defaults resource, falling back to hardcoded values when it is unavailable.
    private static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Hardcoded fallbacks below keep the demo launchable if the resource is missing.
        }
        return new AppConfig(props);
    }

    // Returns the broker's fixed session-start fee.
    public double fixedFee() {
        return getDouble("broker.fixedFee", 50.0);
    }

    // Returns the broker's commission rate applied to settled deal prices.
    public double commissionRate() {
        return getDouble("broker.commissionRate", 0.05);
    }

    // Returns the maximum age of an active negotiation session before timeout.
    public long sessionTimeoutMillis() {
        return getLong("broker.sessionTimeoutMillis", 120000L);
    }

    // Returns how often the broker scans active sessions for timeout.
    public long timeoutScanMillis() {
        return getLong("broker.timeoutScanMillis", 5000L);
    }

    // Builds the negotiation strategy configuration from properties and fallbacks.
    public NegotiationConfig negotiationConfig() {
        NegotiationConfig.Strategy strategy = getStrategy("strategy.initial", NegotiationConfig.Strategy.BOULWARE);
        NegotiationConfig.Strategy switchStrategy = getStrategy("strategy.switch", NegotiationConfig.Strategy.CONCEDER);
        return new NegotiationConfig(
                strategy,
                getInt("strategy.deadlineCycles", 50),
                getDouble("strategy.buyerStartPercent", 0.70),
                getDouble("strategy.dealerReservePercent", 0.70),
                getInt("strategy.maxRoundsPerDealer", 3),
                getInt("strategy.maxSearchRetries", 2),
                getInt("strategy.stuckRoundsBeforeAcceleration", 2),
                getDouble("strategy.manualDealerTargetPercent", 1.0),
                getInt("strategy.switchCycle", 15),
                switchStrategy);
    }

    // Builds the weighted utility preferences used by buyers and dealers.
    public UtilityPreferences utilityPreferences() {
        return new UtilityPreferences(
                getDouble("utility.priceWeight", 0.70),
                getDouble("utility.warrantyWeight", 0.20),
                getDouble("utility.deliveryWeight", 0.10),
                getInt("utility.defaultWarrantyMonths", 12),
                getInt("utility.defaultDeliveryDays", 14));
    }

    // Reads a strategy enum by key, returning the fallback for missing or invalid values.
    private NegotiationConfig.Strategy getStrategy(String key, NegotiationConfig.Strategy fallback) {
        try {
            return NegotiationConfig.Strategy.valueOf(values.getProperty(key, fallback.name()).trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    // Reads an integer property, returning the fallback for missing or invalid values.
    private int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Reads a long property, returning the fallback for missing or invalid values.
    private long getLong(String key, long fallback) {
        try {
            return Long.parseLong(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // Reads a double property, returning the fallback for missing or invalid values.
    private double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
