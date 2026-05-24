package org.example.agents;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads configurable broker, strategy, and utility defaults from the classpath resource. */
public final class AppConfig {
    private static final String RESOURCE_NAME = "negotiation-defaults.properties";
    private static final AppConfig INSTANCE = load();

    private final Properties values;

    /** Stores the loaded property set for typed accessors. */
    private AppConfig(Properties values) {
        this.values = values;
    }

    /** Returns the singleton default application configuration. */
    public static AppConfig defaults() {
        return INSTANCE;
    }

    /** Loads negotiation-defaults.properties and falls back to hardcoded values when absent. */
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

    /** Returns the broker fixed fee charged at session start. */
    public double fixedFee() {
        return getDouble("broker.fixedFee", 50.0);
    }

    /** Returns the broker commission rate charged on successful deals. */
    public double commissionRate() {
        return getDouble("broker.commissionRate", 0.05);
    }

    /** Returns the timeout after which active sessions are closed. */
    public long sessionTimeoutMillis() {
        return getLong("broker.sessionTimeoutMillis", 120000L);
    }

    /** Returns how often the broker scans for timed-out sessions. */
    public long timeoutScanMillis() {
        return getLong("broker.timeoutScanMillis", 5000L);
    }

    /** Builds the default negotiation strategy configuration. */
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

    /** Builds the default multi-attribute utility preferences. */
    public UtilityPreferences utilityPreferences() {
        return new UtilityPreferences(
                getDouble("utility.priceWeight", 0.70),
                getDouble("utility.warrantyWeight", 0.20),
                getDouble("utility.deliveryWeight", 0.10),
                getInt("utility.defaultWarrantyMonths", 12),
                getInt("utility.defaultDeliveryDays", 14));
    }

    /** Reads a strategy enum from properties or returns the fallback. */
    private NegotiationConfig.Strategy getStrategy(String key, NegotiationConfig.Strategy fallback) {
        try {
            return NegotiationConfig.Strategy.valueOf(values.getProperty(key, fallback.name()).trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /** Reads an integer property or returns the fallback. */
    private int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Reads a long property or returns the fallback. */
    private long getLong(String key, long fallback) {
        try {
            return Long.parseLong(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Reads a double property or returns the fallback. */
    private double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(values.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
