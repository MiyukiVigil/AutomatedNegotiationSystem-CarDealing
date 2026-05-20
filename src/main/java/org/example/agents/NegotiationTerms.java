package org.example.agents;

import java.io.Serializable;

/** Immutable multi-attribute offer used in broker-routed negotiation payloads. */
public class NegotiationTerms implements Serializable {
    private final int price;
    private final int warrantyMonths;
    private final int deliveryDays;

    /** Creates terms with non-negative warranty and delivery attributes. */
    public NegotiationTerms(int price, int warrantyMonths, int deliveryDays) {
        this.price = price;
        this.warrantyMonths = Math.max(0, warrantyMonths);
        this.deliveryDays = Math.max(0, deliveryDays);
    }

    /** Creates a legacy price-only offer using configured default warranty and delivery values. */
    public static NegotiationTerms priceOnly(int price) {
        UtilityPreferences defaults = AppConfig.defaults().utilityPreferences();
        return new NegotiationTerms(price, defaults.getDefaultWarrantyMonths(), defaults.getDefaultDeliveryDays());
    }

    /** Returns the offered price. */
    public int getPrice() {
        return price;
    }

    /** Returns the offered warranty length in months. */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    /** Returns the offered delivery time in days. */
    public int getDeliveryDays() {
        return deliveryDays;
    }

    /** Serializes terms as price:warrantyMonths:deliveryDays. */
    public String toPayload() {
        return price + ":" + warrantyMonths + ":" + deliveryDays;
    }

    /** Parses price-only or multi-attribute terms from an ACL payload segment. */
    public static NegotiationTerms fromPayload(String payload) {
        String[] parts = payload.split(":");
        int price = Integer.parseInt(parts[0]);
        UtilityPreferences defaults = AppConfig.defaults().utilityPreferences();
        int warranty = parts.length > 1 ? Integer.parseInt(parts[1]) : defaults.getDefaultWarrantyMonths();
        int delivery = parts.length > 2 ? Integer.parseInt(parts[2]) : defaults.getDefaultDeliveryDays();
        return new NegotiationTerms(price, warranty, delivery);
    }
}
