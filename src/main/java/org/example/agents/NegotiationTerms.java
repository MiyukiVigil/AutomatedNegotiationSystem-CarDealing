package org.example.agents;

import java.io.Serializable;

// Represents the multi-issue terms exchanged in negotiation payloads.
public class NegotiationTerms implements Serializable {
    private final int price;
    private final int warrantyMonths;
    private final int deliveryDays;

    // Creates terms and clamps warranty and delivery values to non-negative ranges.
    public NegotiationTerms(int price, int warrantyMonths, int deliveryDays) {
        this.price = price;
        this.warrantyMonths = Math.max(0, warrantyMonths);
        this.deliveryDays = Math.max(0, deliveryDays);
    }

    // Creates price terms with the configured default warranty and delivery values.
    public static NegotiationTerms priceOnly(int price) {
        UtilityPreferences defaults = AppConfig.defaults().utilityPreferences();
        return new NegotiationTerms(price, defaults.getDefaultWarrantyMonths(), defaults.getDefaultDeliveryDays());
    }

    // Returns the offered purchase price.
    public int getPrice() {
        return price;
    }

    // Returns the warranty length in months.
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    // Returns the delivery time in days.
    public int getDeliveryDays() {
        return deliveryDays;
    }

    // Serializes terms into the protocol payload format "price:warranty:delivery".
    public String toPayload() {
        return price + ":" + warrantyMonths + ":" + deliveryDays;
    }

    // Parses a protocol payload, filling missing warranty or delivery values from defaults.
    public static NegotiationTerms fromPayload(String payload) {
        String[] parts = payload.split(":");
        int price = Integer.parseInt(parts[0]);
        UtilityPreferences defaults = AppConfig.defaults().utilityPreferences();
        int warranty = parts.length > 1 ? Integer.parseInt(parts[1]) : defaults.getDefaultWarrantyMonths();
        int delivery = parts.length > 2 ? Integer.parseInt(parts[2]) : defaults.getDefaultDeliveryDays();
        return new NegotiationTerms(price, warranty, delivery);
    }
}
