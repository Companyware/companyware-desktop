package plugins.community.invoice.domain;

import java.math.BigDecimal;

/** Monetary snapshot of one calculated line. */
public final class CalculatedInvoiceLine {
    private final InvoiceLine source;
    private final BigDecimal net;
    private final BigDecimal tax;
    private final BigDecimal gross;

    CalculatedInvoiceLine(InvoiceLine source, BigDecimal net, BigDecimal tax, BigDecimal gross) {
        this.source = source;
        this.net = net;
        this.tax = tax;
        this.gross = gross;
    }
    public InvoiceLine source() { return source; }
    public BigDecimal net() { return net; }
    public BigDecimal tax() { return tax; }
    public BigDecimal gross() { return gross; }
}

