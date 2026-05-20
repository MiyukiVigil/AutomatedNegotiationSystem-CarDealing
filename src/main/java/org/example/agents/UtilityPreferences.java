package org.example.agents;

import java.io.Serializable;

/** Weighted additive utility model for price, warranty, and delivery terms. */
public class UtilityPreferences implements Serializable {
    private final double priceWeight;
    private final double warrantyWeight;
    private final double deliveryWeight;
    private final int defaultWarrantyMonths;
    private final int defaultDeliveryDays;

    /** Creates normalized utility weights and default non-price attributes. */
    public UtilityPreferences(double priceWeight, double warrantyWeight, double deliveryWeight,
                              int defaultWarrantyMonths, int defaultDeliveryDays) {
        double total = Math.max(0.0001, priceWeight + warrantyWeight + deliveryWeight);
        this.priceWeight = priceWeight / total;
        this.warrantyWeight = warrantyWeight / total;
        this.deliveryWeight = deliveryWeight / total;
        this.defaultWarrantyMonths = Math.max(0, defaultWarrantyMonths);
        this.defaultDeliveryDays = Math.max(0, defaultDeliveryDays);
    }

    /** Scores an offer from the buyer perspective where lower price and delivery are better. */
    public double buyerUtility(NegotiationTerms terms, int maxBudget, int maxWarrantyMonths, int maxDeliveryDays) {
        double priceScore = clamp((double) (maxBudget - terms.getPrice()) / Math.max(1, maxBudget));
        double warrantyScore = clamp((double) terms.getWarrantyMonths() / Math.max(1, maxWarrantyMonths));
        double deliveryScore = clamp((double) (maxDeliveryDays - terms.getDeliveryDays()) / Math.max(1, maxDeliveryDays));
        return priceScore * priceWeight + warrantyScore * warrantyWeight + deliveryScore * deliveryWeight;
    }

    /** Scores an offer from the dealer perspective where higher price and delivery are better. */
    public double dealerUtility(NegotiationTerms terms, int retailPrice, int reservePrice,
                                int maxWarrantyMonths, int maxDeliveryDays) {
        double priceScore = clamp((double) (terms.getPrice() - reservePrice) / Math.max(1, retailPrice - reservePrice));
        double warrantyScore = clamp((double) (maxWarrantyMonths - terms.getWarrantyMonths()) / Math.max(1, maxWarrantyMonths));
        double deliveryScore = clamp((double) terms.getDeliveryDays() / Math.max(1, maxDeliveryDays));
        return priceScore * priceWeight + warrantyScore * warrantyWeight + deliveryScore * deliveryWeight;
    }

    /** Returns the default warranty months for price-only offers. */
    public int getDefaultWarrantyMonths() {
        return defaultWarrantyMonths;
    }

    /** Returns the default delivery days for price-only offers. */
    public int getDefaultDeliveryDays() {
        return defaultDeliveryDays;
    }

    /** Keeps utility components within the inclusive range [0, 1]. */
    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
