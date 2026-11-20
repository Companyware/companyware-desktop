package plugins.community.invoice.infrastructure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import persistence.migration.Migration;
import persistence.migration.MigrationRunner;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.persistence.SqlWork;
import platform.api.plugin.PluginId;
import plugins.community.invoice.application.CreateInvoiceCommand;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoicePdfDto;
import plugins.community.invoice.application.InvoiceLineInput;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.InvoiceBrandingDto;
import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceMailTemplateDto;
import plugins.community.invoice.application.SmtpSettingsDto;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;

public class JdbcInvoiceServiceIntegrationTest {
    private Path temporary;
    private String url;
    private InvoiceService invoices;
    private InvoiceDocumentService documents;
    private EInvoiceService eInvoices;
    private InvoiceMailService mail;
    private FakeMailTransport mailTransport;

    @Before public void prepare() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        temporary = Files.createTempDirectory("companyware-invoice-");
        url = "jdbc:derby:" + temporary.resolve("db") + ";create=true";
        TestDatabase database = new TestDatabase(url);
        database.migrate(PluginId.of("companyware.community.invoice"), Arrays.asList(
                new PluginMigration(1, "invoice tables", migration("createInvoiceTables.sql")),
                new PluginMigration(2, "invoice template", migration("createInvoiceTemplateTable.sql")),
                new PluginMigration(3, "invoice branding", migration("createInvoiceBrandingTable.sql")),
                new PluginMigration(4, "invoice letterhead", migration("addInvoiceLetterheadFields.sql")),
                new PluginMigration(5, "e-invoice archive", migration("createEInvoiceArchive.sql")),
                new PluginMigration(6, "invoice mail", migration("createInvoiceMailTables.sql")),
                new PluginMigration(7, "invoice SMTP settings", migration("createInvoiceSmtpSettings.sql")),
                new PluginMigration(8, "SMTP sender display name", migration("addInvoiceSmtpFromName.sql"))));
        invoices = new JdbcInvoiceService(database);
        documents = new JdbcInvoiceDocumentService(database, invoices);
        eInvoices = new JdbcEInvoiceService(database, invoices, documents);
        mailTransport = new FakeMailTransport();
        mail = new JdbcInvoiceMailService(database, invoices, eInvoices, mailTransport);
    }

    @After public void cleanup() throws Exception {
        if (url != null) try { DriverManager.getConnection(url.replace(";create=true", ";shutdown=true")); }
        catch (Exception expectedShutdown) { }
        if (temporary != null && Files.exists(temporary)) try (Stream<Path> paths = Files.walk(temporary)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
                Files.deleteIfExists(path);
        }
    }

    @Test public void draftHasNoNumberAndFinalizationAllocatesOnlyOnce() {
        InvoiceDto draft = invoices.createDraft(command("Leistung")).valueOrThrow();
        assertEquals("DRAFT", draft.status());
        assertNull(draft.invoiceNumber());
        assertEquals(new BigDecimal("119.00"), draft.gross());

        InvoiceDto finalized = invoices.finalizeDraft(draft.id(), "RE", LocalDate.of(2026, 9, 11)).valueOrThrow();
        assertEquals("FINALIZED", finalized.status());
        assertEquals("RE-2026-000001", finalized.invoiceNumber());

        ApplicationResult<InvoiceDto> second = invoices.finalizeDraft(draft.id(), "RE", LocalDate.of(2026, 9, 11));
        assertFalse(second.isSuccess());
        assertEquals(ApplicationErrorCode.CONFLICT, second.errorCode().get());

        InvoiceDto next = invoices.createDraft(command("Nächste Leistung")).valueOrThrow();
        assertEquals("RE-2026-000002",
                invoices.finalizeDraft(next.id(), "RE", LocalDate.of(2026, 9, 11)).valueOrThrow().invoiceNumber());
    }

    @Test public void concurrentFinalizationProducesDistinctContinuousNumbers() throws Exception {
        final InvoiceDto second = invoices.createDraft(command("B")).valueOrThrow();
        final InvoiceDto third = invoices.createDraft(command("C")).valueOrThrow();
        final InvoiceService otherConnection = new JdbcInvoiceService(new TestDatabase(url));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<String>> numbers = executor.invokeAll(Arrays.asList(
                    finalizeTask(invoices, second.id()), finalizeTask(otherConnection, third.id())));
            String left = numbers.get(0).get();
            String right = numbers.get(1).get();
            assertTrue(left, left.endsWith("000001") || left.endsWith("000002"));
            assertTrue(right, right.endsWith("000001") || right.endsWith("000002"));
            assertFalse(left.equals(right));
        } finally { executor.shutdownNow(); }
    }

    @Test public void draftCanBeListedLoadedAndUpdatedButFinalizedInvoiceCannot() {
        InvoiceDto draft = invoices.createDraft(command("Alt")).valueOrThrow();
        assertEquals(1, invoices.list().valueOrThrow().size());
        assertEquals("Alt", invoices.details(draft.id()).valueOrThrow().lines().get(0).description());

        InvoiceDto updated = invoices.updateDraft(draft.id(), command("Neu")).valueOrThrow();
        assertEquals("Kunde GmbH", updated.customerName());
        assertEquals("Neu", invoices.details(draft.id()).valueOrThrow().lines().get(0).description());

        invoices.finalizeDraft(draft.id(), "RE", LocalDate.of(2026, 9, 12)).valueOrThrow();
        ApplicationResult<InvoiceDto> rejected = invoices.updateDraft(draft.id(), command("Zu spät"));
        assertFalse(rejected.isSuccess());
        assertEquals(ApplicationErrorCode.CONFLICT, rejected.errorCode().get());
        assertEquals("Neu", invoices.details(draft.id()).valueOrThrow().lines().get(0).description());
    }

    @Test public void controlledTemplatePersistsAndProducesPdfFromInvoiceSnapshot() throws Exception {
        InvoiceDto draft = invoices.createDraft(command("Beratung & Support")).valueOrThrow();
        assertEquals(ApplicationErrorCode.CONFLICT, documents.createPdf(draft.id()).errorCode().get());
        InvoicePdfDto draftPreview = documents.previewPdf(draft.id()).valueOrThrow();
        assertEquals("invoice-draft-" + draft.id() + "-preview.pdf", draftPreview.filename());
        assertEquals("%PDF-", new String(draftPreview.content(), 0, 5, StandardCharsets.US_ASCII));
        InvoiceDto finalized = invoices.finalizeDraft(draft.id(), "RE", LocalDate.of(2026, 9, 12)).valueOrThrow();
        assertFalse(documents.template().valueOrThrow().customized());
        InvoicePdfDto defaultPdf = documents.createPdf(finalized.id()).valueOrThrow();
        Path output = java.nio.file.Paths.get("target", "test-output", "invoice-sample.pdf");
        Files.createDirectories(output.getParent());
        Files.write(output, defaultPdf.content());

        String template = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><style>body { color: #222; }</style></head>"
                + "<body><h1>{{invoice.number}}</h1><p>{{seller.name}}</p><table><tbody>{{lines}}</tbody></table>"
                + "<p>{{invoice.gross}} {{invoice.currency}}</p></body></html>";
        assertTrue(documents.saveTemplate(template).valueOrThrow().customized());
        assertEquals(template, documents.template().valueOrThrow().source());
        String samplePreview = documents.previewTemplateHtml(template).valueOrThrow();
        assertTrue(samplePreview.contains("Musterfirma GmbH"));
        assertTrue(samplePreview.contains("RE-2026-000001"));
        InvoicePdfDto samplePdf = documents.previewTemplatePdf(template).valueOrThrow();
        assertEquals("invoice-template-preview.pdf", samplePdf.filename());
        assertEquals("%PDF-", new String(samplePdf.content(), 0, 5, StandardCharsets.US_ASCII));
        assertFalse(documents.previewTemplateHtml("<html><body><script>x</script>{{lines}}</body></html>")
                .isSuccess());
        String preview = documents.previewHtml(finalized.id()).valueOrThrow();
        assertTrue(preview.contains("RE-2026-000001"));
        assertTrue(preview.contains("Beratung &amp; Support"));

        InvoicePdfDto pdf = documents.createPdf(finalized.id()).valueOrThrow();
        assertEquals("RE-2026-000001.pdf", pdf.filename());
        assertEquals("%PDF-", new String(pdf.content(), 0, 5, StandardCharsets.US_ASCII));
        assertArrayEquals(pdf.content(), documents.previewPdf(finalized.id()).valueOrThrow().content());
        assertArrayEquals(pdf.content(), documents.createPdf(finalized.id()).valueOrThrow().content());

        assertFalse(documents.saveTemplate("<html><body><script>x</script>{{lines}}</body></html>").isSuccess());
        assertFalse(documents.restoreDefaultTemplate().valueOrThrow().customized());
        assertFalse(documents.template().valueOrThrow().customized());
    }

    @Test public void validatedLogoPersistsAndIsEmbeddedWithoutExternalAccess() throws Exception {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(20, 10,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setColor(new java.awt.Color(81, 68, 181));
        graphics.fillRect(0, 0, 20, 10);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRect(3, 3, 14, 4);
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", png);
        InvoiceBrandingDto defaults = documents.branding().valueOrThrow();
        assertTrue(defaults.hasLogo());
        assertEquals("image/png", defaults.mediaType());
        assertEquals(65, defaults.widthMillimeters());
        assertEquals("RIGHT", defaults.alignment());
        String defaultPreview = documents.previewTemplateHtml(documents.template().valueOrThrow().source()).valueOrThrow();
        assertTrue(defaultPreview.contains("data:image/png;base64,"));
        assertTrue(defaultPreview.contains("12.09.2026"));
        assertTrue(defaultPreview.contains("Gesamtbetrag"));
        InvoiceBrandingDto letterhead = new InvoiceBrandingDto(png.toByteArray(), "image/png", 35, "RIGHT",
                "Eigene Firma GmbH", "Firmenweg 7\n12345 Berlin", "DE999999999", "Bank · IBAN · BIC");
        assertTrue(documents.saveBranding(letterhead).isSuccess());
        InvoiceBrandingDto stored = documents.branding().valueOrThrow();
        assertTrue(stored.hasLogo());
        assertEquals("Eigene Firma GmbH", stored.sellerName());
        assertEquals("Bank · IBAN · BIC", stored.footer());
        String preview = documents.previewTemplateHtml(documents.template().valueOrThrow().source()).valueOrThrow();
        assertTrue(preview.contains("data:image/png;base64,"));
        assertTrue(preview.contains("width:35mm"));
        assertTrue(preview.contains("text-align:right"));
        assertTrue(preview.contains("Eigene Firma GmbH"));
        assertTrue(preview.contains("Bank · IBAN · BIC"));
        assertTrue(documents.previewBrandingHtml(new InvoiceBrandingDto(png.toByteArray(), "image/png", 25, "LEFT",
                "Ungespeicherte Firma", "Vorschauweg 1", "TAX-1", "Vorschau-Footer"))
                .valueOrThrow().contains("Ungespeicherte Firma"));
        InvoiceDto draft = invoices.createDraft(command("Logo-Testleistung")).valueOrThrow();
        InvoiceDto finalized = invoices.finalizeDraft(draft.id(), "LOGO", LocalDate.of(2026, 9, 12)).valueOrThrow();
        InvoicePdfDto pdf = documents.createPdf(finalized.id()).valueOrThrow();
        Path output = java.nio.file.Paths.get("target", "test-output", "invoice-logo-sample.pdf");
        Files.createDirectories(output.getParent());
        Files.write(output, pdf.content());
        assertFalse(documents.saveLogo(new byte[] { 1, 2, 3 }, "image/png", 35, "LEFT").isSuccess());
        assertFalse(documents.saveLogo(png.toByteArray(), "image/svg+xml", 35, "LEFT").isSuccess());
        InvoiceBrandingDto restored = documents.restoreDefaultLogo().valueOrThrow();
        assertTrue(restored.hasLogo());
        assertEquals(65, restored.widthMillimeters());
        assertEquals("RIGHT", restored.alignment());
        assertEquals("Eigene Firma GmbH", restored.sellerName());
        assertEquals("Bank · IBAN · BIC", restored.footer());
        InvoiceBrandingDto withoutLogo = documents.removeLogo().valueOrThrow();
        assertFalse(withoutLogo.hasLogo());
        assertEquals("Eigene Firma GmbH", withoutLogo.sellerName());
    }

    @Test public void eInvoiceIsArchivedOnceWithEmbeddedXmlAndCorrectionReference() throws Exception {
        InvoiceDto draft = invoices.createDraft(command("E-Rechnungsberatung")).valueOrThrow();
        InvoiceDto finalized = invoices.finalizeDraft(draft.id(), "RE", LocalDate.of(2026, 9, 12)).valueOrThrow();
        EInvoiceArchiveDto first = eInvoices.archive(finalized.id()).valueOrThrow();
        EInvoiceArchiveDto second = eInvoices.archive(finalized.id()).valueOrThrow();
        assertArrayEquals(first.pdf(), second.pdf());
        assertArrayEquals(first.xml(), second.xml());
        String xrechnung = new String(first.xml(), StandardCharsets.UTF_8);
        assertTrue(xrechnung.contains("xrechnung_3.0"));
        assertTrue(xrechnung.contains("RE-2026-000001"));
        try (PDDocument pdf = PDDocument.load(first.pdf())) {
            PDDocumentNameDictionary names = new PDDocumentNameDictionary(pdf.getDocumentCatalog());
            assertTrue(names.getEmbeddedFiles().getNames().containsKey("factur-x.xml"));
            String xmp = new String(pdf.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertTrue(xmp.contains("pdfaid:part=\"3\""));
            assertTrue(xmp.contains("ConformanceLevel=\"EN 16931\""));
        }
        Path output = java.nio.file.Paths.get("target", "test-output", "zugferd-sample.pdf");
        Files.createDirectories(output.getParent()); Files.write(output, first.pdf());

        plugins.community.invoice.application.InvoiceDetailsDto correction =
                eInvoices.createCorrectionDraft(finalized.id(), "Leistung entfällt").valueOrThrow();
        assertEquals("CANCELLED", invoices.find(finalized.id()).valueOrThrow().status());
        InvoiceDto credit = invoices.finalizeDraft(correction.summary().id(), "GS", LocalDate.of(2026, 9, 13)).valueOrThrow();
        String creditXml = new String(eInvoices.archive(credit.id()).valueOrThrow().xml(), StandardCharsets.UTF_8);
        assertTrue(creditXml.contains("CreditNoteTypeCode>381"));
        assertTrue(creditXml.contains("RE-2026-000001"));
    }

    @Test public void reviewedMailUsesEditableTemplateAndArchivedAttachments() {
        InvoiceDto finalized = invoices.finalizeDraft(invoices.createDraft(command("Mail-Test")).valueOrThrow().id(),
                "RE", LocalDate.of(2026, 9, 12)).valueOrThrow();
        InvoiceMailTemplateDto defaults = mail.template().valueOrThrow();
        assertTrue(defaults.subject().contains("{{invoice.number}}"));
        assertFalse(mail.saveTemplate(new InvoiceMailTemplateDto("{{unknown}}", "Text")).isSuccess());
        mail.saveTemplate(new InvoiceMailTemplateDto("Beleg {{invoice.number}}", "Hallo {{customer.name}}")).valueOrThrow();
        InvoiceMailDraftDto rendered = mail.draft(finalized.id()).valueOrThrow();
        assertEquals("Beleg RE-2026-000001", rendered.subject());
        assertEquals("Hallo Kunde GmbH", rendered.body());
        InvoiceMailDraftDto reviewed = new InvoiceMailDraftDto(finalized.id(), "rechnung@example.org",
                rendered.subject(), rendered.body() + "\nGeprüft");
        assertTrue(mail.send(reviewed, true).isSuccess());
        assertEquals("rechnung@example.org", mailTransport.draft.recipient());
        assertTrue(mailTransport.xml);
        assertEquals("%PDF-", new String(mailTransport.artifact.pdf(), 0, 5, StandardCharsets.US_ASCII));
        assertTrue(new String(mailTransport.artifact.xml(), StandardCharsets.UTF_8).contains("RE-2026-000001"));
    }

    @Test public void smtpEndpointSettingsPersistAndCanBeTestedBeforeSaving() {
        SmtpSettingsDto candidate = new SmtpSettingsDto("smtp.example.org", 587,
                "Mathias Bauer - mbdus", "rechnung@example.org", "mailer", true, false);
        assertTrue(mail.testSmtpSettings(candidate, "session-secret".toCharArray()).isSuccess());
        assertEquals("smtp.example.org", mailTransport.tested.host());
        assertEquals("session-secret", mailTransport.password);
        SmtpSettingsDto saved = mail.saveSmtpSettings(candidate, new char[0]).valueOrThrow();
        assertEquals("smtp.example.org", saved.host());
        assertEquals(587, saved.port());
        assertEquals("Mathias Bauer - mbdus", mail.smtpSettings().valueOrThrow().fromName());
        assertEquals("mailer", mail.smtpSettings().valueOrThrow().username());
    }

    private Callable<String> finalizeTask(final InvoiceService service, final long id) {
        return () -> service.finalizeDraft(id, "RE", LocalDate.of(2026, 1, 1)).valueOrThrow().invoiceNumber();
    }

    private CreateInvoiceCommand command(String description) {
        return new CreateInvoiceCommand("Muster GmbH", "Musterweg 1\n12345 Berlin", "DE123456789",
                "Kunde GmbH", "Kundenweg 2\n54321 Bonn", LocalDate.of(2026, 9, 10), "EUR",
                "Zahlbar innerhalb von 14 Tagen", Collections.singletonList(
                        new InvoiceLineInput(description, BigDecimal.ONE, new BigDecimal("100.00"), new BigDecimal("19"))));
    }

    private String migration(String filename) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "plugins/community/invoice/migration/" + filename)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static final class TestDatabase implements PluginDatabase {
        private final String url;
        private TestDatabase(String url) { this.url = url; }
        @Override public void migrate(PluginId owner, List<PluginMigration> migrations) throws Exception {
            try (Connection connection = DriverManager.getConnection(url)) {
                java.util.ArrayList<Migration> values = new java.util.ArrayList<Migration>();
                for (PluginMigration migration : migrations) values.add(new Migration(owner, migration.version(),
                        migration.description(), migration.sql(), migration.acceptedPreviousChecksums()));
                new MigrationRunner().migrate(connection, values);
            }
        }
        @Override public void baselineIfTableExists(PluginId owner, PluginMigration migration, String tableName) { }
        @Override public <T> T read(SqlWork<T> work) throws Exception {
            try (Connection connection = DriverManager.getConnection(url)) { return work.execute(connection); }
        }
        @Override public <T> T write(SqlWork<T> work) throws Exception {
            try (Connection connection = DriverManager.getConnection(url)) {
                connection.setAutoCommit(false);
                try {
                    T result = work.execute(connection);
                    connection.commit();
                    return result;
                } catch (Exception failure) {
                    connection.rollback();
                    throw failure;
                }
            }
        }
    }

    private static final class FakeMailTransport implements MailTransport {
        InvoiceMailDraftDto draft; EInvoiceArchiveDto artifact; boolean xml;
        SmtpSettingsDto tested; String password;
        @Override public void send(InvoiceMailDraftDto draft, EInvoiceArchiveDto artifact, boolean xml) {
            this.draft = draft; this.artifact = artifact; this.xml = xml;
        }
        @Override public void test(SmtpSettingsDto settings, char[] password) {
            this.tested = settings; this.password = new String(password);
        }
    }
}
