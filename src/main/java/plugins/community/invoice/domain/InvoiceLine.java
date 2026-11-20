package plugins.community.invoice.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Immutable, unrounded invoice-line input. */
public final class InvoiceLine {
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitNet;
    private final BigDecimal taxRate;

    public InvoiceLine(String description, BigDecimal quantity, BigDecimal unitNet, BigDecimal taxRate) {
        this.description = required(description, "description");
        this.quantity = Objects.requireNonNull(quantity, "quantity");
        this.unitNet = Objects.requireNonNull(unitNet, "unitNet");
        this.taxRate = Objects.requireNonNull(taxRate, "taxRate");
        if (quantity.signum() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (unitNet.signum() < 0) throw new IllegalArgumentException("unitNet must not be negative");
        if (taxRate.signum() < 0 || taxRate.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("taxRate must be between 0 and 100");
    }

    public String description() { return description; }
    public BigDecimal quantity() { return quantity; }
    public BigDecimal unitNet() { return unitNet; }
    public BigDecimal taxRate() { return taxRate; }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}

