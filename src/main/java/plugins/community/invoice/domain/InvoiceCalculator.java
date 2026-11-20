package plugins.community.invoice.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministic line-level tax calculation with commercial cent rounding. */
public final class InvoiceCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public InvoiceTotals calculate(List<InvoiceLine> sourceLines) {
        if (sourceLines == null || sourceLines.isEmpty())
            throw new IllegalArgumentException("at least one invoice line is required");
        List<CalculatedInvoiceLine> lines = new ArrayList<CalculatedInvoiceLine>();
        Map<BigDecimal, BigDecimal[]> groups = new LinkedHashMap<BigDecimal, BigDecimal[]>();
        BigDecimal totalNet = money(BigDecimal.ZERO);
        BigDecimal totalTax = money(BigDecimal.ZERO);
        for (InvoiceLine source : sourceLines) {
            if (source == null) throw new IllegalArgumentException("invoice line must not be null");
            BigDecimal net = money(source.quantity().multiply(source.unitNet()));
            BigDecimal tax = money(net.multiply(source.taxRate()).divide(HUNDRED));
            BigDecimal gross = net.add(tax);
            lines.add(new CalculatedInvoiceLine(source, net, tax, gross));
            BigDecimal rate = source.taxRate().stripTrailingZeros();
            BigDecimal[] group = groups.get(rate);
            if (group == null) groups.put(rate, group = new BigDecimal[] { money(BigDecimal.ZERO), money(BigDecimal.ZERO) });
            group[0] = group[0].add(net);
            group[1] = group[1].add(tax);
            totalNet = totalNet.add(net);
            totalTax = totalTax.add(tax);
        }
        List<TaxSummary> summaries = new ArrayList<TaxSummary>();
        for (Map.Entry<BigDecimal, BigDecimal[]> group : groups.entrySet())
            summaries.add(new TaxSummary(group.getKey(), group.getValue()[0], group.getValue()[1]));
        return new InvoiceTotals(lines, summaries, totalNet, totalTax, totalNet.add(totalTax));
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}

