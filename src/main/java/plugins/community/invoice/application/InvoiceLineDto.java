package plugins.community.invoice.application;

import java.math.BigDecimal;
import plugins.community.invoice.domain.InvoiceLine;

/** Immutable invoice line exposed to presentation adapters. */
public final class InvoiceLineDto {
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitNet;
    private final BigDecimal taxRate;
    private final BigDecimal net;
    private final BigDecimal tax;
    private final BigDecimal gross;

    public InvoiceLineDto(String description, BigDecimal quantity, BigDecimal unitNet,
            BigDecimal taxRate, BigDecimal net, BigDecimal tax, BigDecimal gross) {
        this.description = description;
        this.quantity = quantity;
        this.unitNet = unitNet;
        this.taxRate = taxRate;
        this.net = net;
        this.tax = tax;
        this.gross = gross;
    }
    public String description() { return description; }
    public BigDecimal quantity() { return quantity; }
    public BigDecimal unitNet() { return unitNet; }
    public BigDecimal taxRate() { return taxRate; }
    public BigDecimal net() { return net; }
    public BigDecimal tax() { return tax; }
    public BigDecimal gross() { return gross; }
    public InvoiceLine toDomain() { return new InvoiceLine(description, quantity, unitNet, taxRate); }
}

