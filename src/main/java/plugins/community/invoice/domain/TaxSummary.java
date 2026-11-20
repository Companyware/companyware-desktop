package plugins.community.invoice.domain;

import java.math.BigDecimal;

/** Totals for one tax rate. */
public final class TaxSummary {
    private final BigDecimal rate;
    private final BigDecimal net;
    private final BigDecimal tax;

    TaxSummary(BigDecimal rate, BigDecimal net, BigDecimal tax) {
        this.rate = rate;
        this.net = net;
        this.tax = tax;
    }
    public BigDecimal rate() { return rate; }
    public BigDecimal net() { return net; }
    public BigDecimal tax() { return tax; }
}

