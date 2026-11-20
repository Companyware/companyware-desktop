package plugins.community.invoice.domain;

import static org.junit.Assert.assertEquals;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.Test;

public class InvoiceCalculatorTest {
    @Test public void calculatesLinesAndGroupsTaxUsingCommercialCentRounding() {
        InvoiceTotals totals = new InvoiceCalculator().calculate(Arrays.asList(
                new InvoiceLine("Beratung", new BigDecimal("1.5"), new BigDecimal("100.00"), new BigDecimal("19")),
                new InvoiceLine("Buch", new BigDecimal("2"), new BigDecimal("10.005"), new BigDecimal("7")),
                new InvoiceLine("Porto", BigDecimal.ONE, new BigDecimal("4.99"), new BigDecimal("19"))));

        assertEquals(new BigDecimal("175.00"), totals.net());
        assertEquals(new BigDecimal("30.85"), totals.tax());
        assertEquals(new BigDecimal("205.85"), totals.gross());
        assertEquals(2, totals.taxes().size());
        assertEquals(new BigDecimal("154.99"), totals.taxes().get(0).net());
        assertEquals(new BigDecimal("29.45"), totals.taxes().get(0).tax());
        assertEquals(new BigDecimal("20.01"), totals.taxes().get(1).net());
        assertEquals(new BigDecimal("1.40"), totals.taxes().get(1).tax());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptyInvoices() { new InvoiceCalculator().calculate(java.util.Collections.emptyList()); }
}
