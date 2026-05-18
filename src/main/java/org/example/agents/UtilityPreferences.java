package org.example.agents;

import java.io.Serializable;

// Scores negotiation terms from buyer and dealer perspectives using weighted issues.
public class UtilityPreferences implements Serializable {
    private final double priceWeight;
    private final double warrantyWeight;
    private final double deliveryWeight;
    private final int defaultWarrantyMonths;
    private final int defaultDeliveryDays;

    // Normalizes issue weights and stores default non-price terms.
    public UtilityPreferences(double priceWeight, double warrantyWeight, double deliveryWeight,
                              int defaultWarrantyMonths, int defaultDeliveryDays) {
        double total = Math.max(0.0001, priceWeight + warrantyWeight + deliveryWeight);
        this.priceWeight = priceWeight / total;
        this.warrantyWeight = warrantyWeight / total;
        this.deliveryWeight = deliveryWeight / total;
        this.defaultWarrantyMonths = Math.max(0, defaultWarrantyMonths);
        this.defaultDeliveryDays = Math.max(0, defaultDeliveryDays);
    }

    // Computes buyer utility where lower price, longer warranty, and faster delivery score higher.
    public double buyerUtility(NegotiationTerms terms, int maxBudget, int maxWarrantyMonths, int maxDeliveryDays) {
        double priceScore = clamp((double) (maxBudget - terms.getPrice()) / Math.max(1, maxBudget));
        double warrantyScore = clamp((double) terms.getWarrantyMonths() / Math.max(1, maxWarrantyMonths));
        double deliveryScore = clamp((double) (maxDeliveryDays - terms.getDeliveryDays()) / Math.max(1, maxDeliveryDays));
        return priceScore * priceWeight + warrantyScore * warrantyWeight + deliveryScore * deliveryWeight;
    }

    // Computes dealer utility where higher price, shorter warranty, and slower delivery score higher.
    public double dealerUtility(NegotiationTerms terms, int retailPrice, int reservePrice,
                                int maxWarrantyMonths, int maxDeliveryDays) {
        double priceScore = clamp((double) (terms.getPrice() - reservePrice) / Math.max(1, retailPrice - reservePrice));
        double warrantyScore = clamp((double) (maxWarrantyMonths - terms.getWarrantyMonths()) / Math.max(1, maxWarrantyMonths));
        double deliveryScore = clamp((double) terms.getDeliveryDays() / Math.max(1, maxDeliveryDays));
        return priceScore * priceWeight + warrantyScore * warrantyWeight + deliveryScore * deliveryWeight;
    }

    // Returns the default warranty used when payloads omit warranty.
    public int getDefaultWarrantyMonths() {
        return defaultWarrantyMonths;
    }

    // Returns the default delivery time used when payloads omit delivery.
    public int getDefaultDeliveryDays() {
        return defaultDeliveryDays;
    }

    // Restricts a utility component to the inclusive range from 0.0 to 1.0.
    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
