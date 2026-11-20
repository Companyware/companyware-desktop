package plugins.community.invoice.application;

import java.math.BigDecimal;
import plugins.community.invoice.domain.InvoiceLine;

/** Immutable line input accepted by invoice draft use cases. */
public final class InvoiceLineInput {
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitNet;
    private final BigDecimal taxRate;

    public InvoiceLineInput(String description, BigDecimal quantity, BigDecimal unitNet, BigDecimal taxRate) {
        this.description = description;
        this.quantity = quantity;
        this.unitNet = unitNet;
        this.taxRate = taxRate;
    }
    public String description() { return description; }
    public BigDecimal quantity() { return quantity; }
    public BigDecimal unitNet() { return unitNet; }
    public BigDecimal taxRate() { return taxRate; }
    public InvoiceLine toDomain() { return new InvoiceLine(description, quantity, unitNet, taxRate); }
}

