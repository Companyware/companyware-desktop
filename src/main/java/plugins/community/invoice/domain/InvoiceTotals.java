package plugins.community.invoice.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** Immutable calculated lines and totals used as the persisted invoice snapshot. */
public final class InvoiceTotals {
    private final List<CalculatedInvoiceLine> lines;
    private final List<TaxSummary> taxes;
    private final BigDecimal net;
    private final BigDecimal tax;
    private final BigDecimal gross;

    InvoiceTotals(List<CalculatedInvoiceLine> lines, List<TaxSummary> taxes,
            BigDecimal net, BigDecimal tax, BigDecimal gross) {
        this.lines = Collections.unmodifiableList(lines);
        this.taxes = Collections.unmodifiableList(taxes);
        this.net = net;
        this.tax = tax;
        this.gross = gross;
    }
    public List<CalculatedInvoiceLine> lines() { return lines; }
    public List<TaxSummary> taxes() { return taxes; }
    public BigDecimal net() { return net; }
    public BigDecimal tax() { return tax; }
    public BigDecimal gross() { return gross; }
}

