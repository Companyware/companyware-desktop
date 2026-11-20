package plugins.community.invoice.infrastructure;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.Test;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoiceLineDto;

public class InvoiceTemplateEngineTest {
    private final InvoiceTemplateEngine engine = new InvoiceTemplateEngine();

    @Test public void escapesSnapshotValuesAndExpandsControlledLineRows() {
        String source = "<html><body><p>{{customer.name}}</p><table><tbody>{{lines}}</tbody></table></body></html>";
        String rendered = engine.render(source, details());
        assertTrue(rendered.contains("Kunde &lt;Nord&gt;"));
        assertTrue(rendered.contains("Analyse &amp; Planung"));
        assertTrue(rendered.contains("<tr><td>"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsExecutableElements() {
        engine.validate("<html><body><script>alert(1)</script>{{lines}}</body></html>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsExternalCssResources() {
        engine.validate("<html><head><style>@import 'https://example.test/x.css';</style></head>"
                + "<body>{{lines}}</body></html>");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownPlaceholders() {
        engine.validate("<html><body>{{customer.secret}}{{lines}}</body></html>");
    }

    @Test public void rendersFooterInOlderTemplatesWithoutTheNewToken() {
        String source = "<html><body><table><tbody>{{lines}}</tbody></table></body></html>";
        String rendered = engine.render(source, details(), "", "Bank & Partner");
        assertTrue(rendered.contains("<div class=\"footer\">Bank &amp; Partner</div>"));
    }

    private InvoiceDetailsDto details() {
        InvoiceDto summary = new InvoiceDto(7L, "FINALIZED", "RE-2026-000007", "Kunde <Nord>",
                LocalDate.of(2026, 9, 12), new BigDecimal("100.00"), new BigDecimal("19.00"),
                new BigDecimal("119.00"));
        InvoiceLineDto line = new InvoiceLineDto("Analyse & Planung", BigDecimal.ONE,
                new BigDecimal("100.00"), new BigDecimal("19"), new BigDecimal("100.00"),
                new BigDecimal("19.00"), new BigDecimal("119.00"));
        return new InvoiceDetailsDto(summary, "Muster GmbH", "Musterweg 1", "DE123", "Kundenweg 2",
                LocalDate.of(2026, 9, 10), "EUR", "14 Tage", Collections.singletonList(line));
    }
}
