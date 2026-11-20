package plugins.community.invoice.infrastructure;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoiceLineDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoicePdfDto;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.InvoiceTemplateDto;
import plugins.community.invoice.application.InvoiceBrandingDto;

/** Derby-backed template store and deterministic XHTML-to-PDF adapter. */
public final class JdbcInvoiceDocumentService implements InvoiceDocumentService {
    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private final PluginDatabase database;
    private final InvoiceService invoices;
    private final InvoiceTemplateEngine engine;
    private final String defaultTemplate;
    private final InvoiceBrandingDto defaultBranding;

    public JdbcInvoiceDocumentService(PluginDatabase database, InvoiceService invoices) throws IOException {
        this(database, invoices, new InvoiceTemplateEngine(), resource("default-template.xhtml"),
                new InvoiceBrandingDto(resourceBytes("default-logo.png"), "image/png", 65, "RIGHT"));
    }

    JdbcInvoiceDocumentService(PluginDatabase database, InvoiceService invoices,
            InvoiceTemplateEngine engine, String defaultTemplate) {
        this(database, invoices, engine, defaultTemplate, new InvoiceBrandingDto(null, null, 40, "LEFT"));
    }

    JdbcInvoiceDocumentService(PluginDatabase database, InvoiceService invoices,
            InvoiceTemplateEngine engine, String defaultTemplate, InvoiceBrandingDto defaultBranding) {
        this.database = database;
        this.invoices = invoices;
        this.engine = engine;
        this.defaultTemplate = defaultTemplate;
        this.defaultBranding = defaultBranding;
        engine.validate(defaultTemplate);
    }

    @Override public ApplicationResult<InvoiceTemplateDto> template() {
        try {
            InvoiceTemplateDto stored = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT TEMPLATE_HTML,UPDATED_AT FROM CW_INVOICE_TEMPLATE WHERE ID=1");
                     ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) return null;
                    Timestamp updated = rows.getTimestamp(2);
                    return new InvoiceTemplateDto(rows.getString(1), true,
                            updated == null ? null : updated.toLocalDateTime());
                }
            });
            return ApplicationResult.success(stored == null
                    ? new InvoiceTemplateDto(defaultTemplate, false, null) : stored);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice template is unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceTemplateDto> saveTemplate(String source) {
        try {
            engine.validate(source);
            final String normalized = source.trim();
            final Timestamp now = new Timestamp(System.currentTimeMillis());
            database.write(connection -> {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE CW_INVOICE_TEMPLATE SET TEMPLATE_HTML=?,UPDATED_AT=? WHERE ID=1")) {
                    update.setString(1, normalized);
                    update.setTimestamp(2, now);
                    if (update.executeUpdate() == 0) {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO CW_INVOICE_TEMPLATE (ID,TEMPLATE_HTML,UPDATED_AT) VALUES (1,?,?)")) {
                            insert.setString(1, normalized);
                            insert.setTimestamp(2, now);
                            insert.executeUpdate();
                        }
                    }
                }
                return null;
            });
            return ApplicationResult.success(new InvoiceTemplateDto(normalized, true, now.toLocalDateTime()));
        } catch (IllegalArgumentException validation) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, validation.getMessage());
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice template could not be saved");
        }
    }

    @Override public ApplicationResult<InvoiceTemplateDto> restoreDefaultTemplate() {
        try {
            database.write(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM CW_INVOICE_TEMPLATE WHERE ID=1")) {
                    statement.executeUpdate();
                }
                return null;
            });
            return ApplicationResult.success(new InvoiceTemplateDto(defaultTemplate, false, null));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE,
                    "Default invoice template could not be restored");
        }
    }

    @Override public ApplicationResult<InvoiceBrandingDto> branding() {
        try {
            InvoiceBrandingDto value = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT LOGO_DATA,LOGO_MEDIA_TYPE,LOGO_WIDTH_MM,LOGO_ALIGNMENT,SELLER_NAME,SELLER_ADDRESS,SELLER_TAX_ID,FOOTER_TEXT FROM CW_INVOICE_BRANDING WHERE ID=1");
                     ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) return null;
                    return new InvoiceBrandingDto(rows.getBytes(1), rows.getString(2), rows.getInt(3), rows.getString(4),
                            rows.getString(5), rows.getString(6), rows.getString(7), rows.getString(8));
                }
            });
            return ApplicationResult.success(value == null ? defaultBranding : value);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice branding is unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceBrandingDto> saveLogo(byte[] content, String mediaType,
            int widthMillimeters, String alignment) {
        ApplicationResult<InvoiceBrandingDto> current = branding();
        if (!current.isSuccess()) return current;
        InvoiceBrandingDto value = current.valueOrThrow();
        return saveBranding(new InvoiceBrandingDto(content, mediaType, widthMillimeters, alignment,
                value.sellerName(), value.sellerAddress(), value.sellerTaxId(), value.footer()));
    }

    @Override public ApplicationResult<InvoiceBrandingDto> saveBranding(InvoiceBrandingDto branding) {
        try {
            final InvoiceBrandingDto value = validateBranding(branding);
            final Timestamp now = new Timestamp(System.currentTimeMillis());
            database.write(connection -> {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE CW_INVOICE_BRANDING SET LOGO_DATA=?,LOGO_MEDIA_TYPE=?,LOGO_WIDTH_MM=?,LOGO_ALIGNMENT=?,SELLER_NAME=?,SELLER_ADDRESS=?,SELLER_TAX_ID=?,FOOTER_TEXT=?,UPDATED_AT=? WHERE ID=1")) {
                    bindBranding(update, value, now, 1);
                    if (update.executeUpdate() == 0) {
                        try (PreparedStatement insert = connection.prepareStatement(
                                "INSERT INTO CW_INVOICE_BRANDING (LOGO_DATA,LOGO_MEDIA_TYPE,LOGO_WIDTH_MM,LOGO_ALIGNMENT,SELLER_NAME,SELLER_ADDRESS,SELLER_TAX_ID,FOOTER_TEXT,UPDATED_AT,ID) VALUES (?,?,?,?,?,?,?,?,?,1)")) {
                            bindBranding(insert, value, now, 1);
                            insert.executeUpdate();
                        }
                    }
                }
                return null;
            });
            return ApplicationResult.success(value);
        } catch (IllegalArgumentException validation) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, validation.getMessage());
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice letterhead could not be saved");
        }
    }

    @Override public ApplicationResult<InvoiceBrandingDto> removeLogo() {
        ApplicationResult<InvoiceBrandingDto> current = branding();
        if (!current.isSuccess()) return current;
        InvoiceBrandingDto value = current.valueOrThrow();
        return saveBranding(new InvoiceBrandingDto(null, null, value.widthMillimeters(), value.alignment(),
                value.sellerName(), value.sellerAddress(), value.sellerTaxId(), value.footer()));
    }

    @Override public ApplicationResult<InvoiceBrandingDto> restoreDefaultLogo() {
        ApplicationResult<InvoiceBrandingDto> current = branding();
        if (!current.isSuccess()) return current;
        InvoiceBrandingDto value = current.valueOrThrow();
        return saveBranding(new InvoiceBrandingDto(defaultBranding.logo(), defaultBranding.mediaType(),
                defaultBranding.widthMillimeters(), defaultBranding.alignment(), value.sellerName(),
                value.sellerAddress(), value.sellerTaxId(), value.footer()));
    }

    @Override public ApplicationResult<String> previewBrandingHtml(InvoiceBrandingDto branding) {
        ApplicationResult<InvoiceTemplateDto> template = template();
        if (!template.isSuccess()) return ApplicationResult.failure(template.errorCode().get(), template.message());
        try {
            InvoiceBrandingDto value = validateBranding(branding);
            return ApplicationResult.success(engine.render(template.valueOrThrow().source(), sampleInvoice(value),
                    logoMarkup(value), value.footer()));
        } catch (IllegalArgumentException invalid) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage());
        }
    }

    @Override public ApplicationResult<String> previewTemplateHtml(String source) {
        try {
            ApplicationResult<InvoiceBrandingDto> branding = branding();
            if (!branding.isSuccess()) return ApplicationResult.failure(branding.errorCode().get(), branding.message());
            InvoiceBrandingDto value = branding.valueOrThrow();
            return ApplicationResult.success(engine.render(source, sampleInvoice(value), logoMarkup(value), value.footer()));
        } catch (IllegalArgumentException invalid) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage());
        }
    }

    @Override public ApplicationResult<InvoicePdfDto> previewTemplatePdf(String source) {
        ApplicationResult<String> preview = previewTemplateHtml(source);
        if (!preview.isSuccess()) return ApplicationResult.failure(preview.errorCode().get(), preview.message());
        try {
            return ApplicationResult.success(new InvoicePdfDto("invoice-template-preview.pdf",
                    renderPdf(preview.valueOrThrow(), sampleInvoice().summary().issueDate())));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE,
                    "Invoice template PDF preview could not be generated");
        }
    }

    @Override public ApplicationResult<String> previewHtml(long invoiceId) {
        ApplicationResult<InvoiceDetailsDto> details = invoices.details(invoiceId);
        if (!details.isSuccess()) return ApplicationResult.failure(details.errorCode().get(), details.message());
        ApplicationResult<InvoiceTemplateDto> template = template();
        if (!template.isSuccess()) return ApplicationResult.failure(template.errorCode().get(), template.message());
        try {
            ApplicationResult<InvoiceBrandingDto> branding = branding();
            if (!branding.isSuccess()) return ApplicationResult.failure(branding.errorCode().get(), branding.message());
            InvoiceBrandingDto value = branding.valueOrThrow();
            return ApplicationResult.success(engine.render(template.valueOrThrow().source(), details.valueOrThrow(),
                    logoMarkup(value), value.footer()));
        } catch (IllegalArgumentException invalid) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage());
        }
    }

    @Override public ApplicationResult<InvoicePdfDto> createPdf(long invoiceId) {
        ApplicationResult<plugins.community.invoice.application.InvoiceDto> summary = invoices.find(invoiceId);
        if (!summary.isSuccess()) return ApplicationResult.failure(summary.errorCode().get(), summary.message());
        if (!"FINALIZED".equals(summary.valueOrThrow().status()))
            return ApplicationResult.failure(ApplicationErrorCode.CONFLICT,
                    "Only a finalized invoice can be exported as PDF");
        ApplicationResult<InvoicePdfDto> preview = previewPdf(invoiceId);
        if (!preview.isSuccess()) return preview;
        String number = summary.valueOrThrow().invoiceNumber();
        String filename = number == null ? "invoice-draft-" + invoiceId + ".pdf"
                : number.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
        return ApplicationResult.success(new InvoicePdfDto(filename, preview.valueOrThrow().content()));
    }

    @Override public ApplicationResult<InvoicePdfDto> previewPdf(long invoiceId) {
        ApplicationResult<plugins.community.invoice.application.InvoiceDto> summary = invoices.find(invoiceId);
        if (!summary.isSuccess()) return ApplicationResult.failure(summary.errorCode().get(), summary.message());
        ApplicationResult<String> preview = previewHtml(invoiceId);
        if (!preview.isSuccess()) return ApplicationResult.failure(preview.errorCode().get(), preview.message());
        try {
            byte[] normalized = renderPdf(preview.valueOrThrow(), summary.valueOrThrow().issueDate());
            String number = summary.valueOrThrow().invoiceNumber();
            String filename = number == null ? "invoice-draft-" + invoiceId + "-preview.pdf"
                    : number.replaceAll("[^A-Za-z0-9._-]", "_") + "-preview.pdf";
            return ApplicationResult.success(new InvoicePdfDto(filename, normalized));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE,
                    "Invoice PDF preview could not be generated");
        }
    }

    private static byte[] renderPdf(String html, LocalDate issueDate) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            return normalize(output.toByteArray(), html, issueDate);
        }
    }

    private static byte[] normalize(byte[] rendered, String html, java.time.LocalDate issueDate) throws Exception {
        try (PDDocument document = PDDocument.load(rendered);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            java.time.LocalDate stableDate = issueDate == null ? java.time.LocalDate.of(1970, 1, 1) : issueDate;
            Calendar timestamp = GregorianCalendar.from(stableDate.atStartOfDay(ZoneOffset.UTC));
            timestamp.setTimeZone(TimeZone.getTimeZone("UTC"));
            PDDocumentInformation information = new PDDocumentInformation();
            information.setProducer("Companyware Invoice Renderer");
            information.setCreationDate(timestamp);
            information.setModificationDate(timestamp);
            document.setDocumentInformation(information);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(html.getBytes(StandardCharsets.UTF_8));
            byte[] identifier = java.util.Arrays.copyOf(digest, 16);
            COSArray ids = new COSArray();
            ids.add(new COSString(identifier));
            ids.add(new COSString(identifier));
            document.getDocument().setDocumentID(ids);
            document.save(output);
            return output.toByteArray();
        }
    }

    private InvoiceDetailsDto sampleInvoice() { return sampleInvoice(defaultBranding); }

    private static InvoiceDetailsDto sampleInvoice(InvoiceBrandingDto branding) {
        BigDecimal net = new BigDecimal("1000.00");
        BigDecimal tax = new BigDecimal("190.00");
        InvoiceDto summary = new InvoiceDto(0L, "FINALIZED", "RE-2026-000001", "Musterkunde GmbH",
                LocalDate.of(2026, 9, 12), net, tax, new BigDecimal("1190.00"));
        InvoiceLineDto line = new InvoiceLineDto("Beratung und Umsetzung", new BigDecimal("10"),
                new BigDecimal("100.00"), new BigDecimal("19"), net, tax, new BigDecimal("1190.00"));
        String sellerName = empty(branding.sellerName()) ? "Musterfirma GmbH" : branding.sellerName();
        String sellerAddress = empty(branding.sellerAddress()) ? "Musterstraße 1\n12345 Musterstadt" : branding.sellerAddress();
        String sellerTaxId = empty(branding.sellerTaxId()) ? "DE123456789" : branding.sellerTaxId();
        return new InvoiceDetailsDto(summary, sellerName,
                sellerAddress, sellerTaxId,
                "Kundenweg 2\n54321 Kundenstadt", LocalDate.of(2026, 9, 10), "EUR",
                "Zahlbar innerhalb von 14 Tagen ohne Abzug.", Arrays.asList(line));
    }

    private static InvoiceBrandingDto validateBranding(InvoiceBrandingDto branding) {
        if (branding == null) throw new IllegalArgumentException("letterhead is required");
        byte[] content = branding.logo();
        String mediaType = branding.mediaType();
        int width = branding.widthMillimeters();
        String alignment = branding.alignment();
        if (width < 10 || width > 80) throw new IllegalArgumentException("logo width must be between 10 and 80 mm");
        String aligned = alignment == null ? "" : alignment.toUpperCase(java.util.Locale.ROOT);
        if (!("LEFT".equals(aligned) || "CENTER".equals(aligned) || "RIGHT".equals(aligned)))
            throw new IllegalArgumentException("logo alignment is invalid");
        String type = mediaType == null ? null : mediaType.toLowerCase(java.util.Locale.ROOT);
        if (content == null || content.length == 0) {
            type = null;
        } else {
        if (content.length > MAX_LOGO_BYTES) throw new IllegalArgumentException("logo exceeds 2 MiB");
        if (!("image/png".equals(type) || "image/jpeg".equals(type)))
            throw new IllegalArgumentException("logo must be PNG or JPEG");
        try {
            java.awt.image.BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1)
                throw new IllegalArgumentException("logo image is invalid");
            if (image.getWidth() > 6000 || image.getHeight() > 6000)
                throw new IllegalArgumentException("logo dimensions exceed 6000 pixels");
        } catch (IOException invalid) { throw new IllegalArgumentException("logo image is invalid", invalid); }
        }
        String sellerName = bounded(branding.sellerName(), 200, "seller name");
        String sellerAddress = bounded(branding.sellerAddress(), 1000, "seller address");
        String sellerTaxId = bounded(branding.sellerTaxId(), 100, "seller tax ID");
        String footer = bounded(branding.footer(), 2000, "footer");
        return new InvoiceBrandingDto(content, type, width, aligned, sellerName, sellerAddress, sellerTaxId, footer);
    }

    private static void bindBranding(PreparedStatement statement, InvoiceBrandingDto value,
            Timestamp timestamp, int offset) throws java.sql.SQLException {
        statement.setBytes(offset, value.logo());
        statement.setString(offset + 1, value.mediaType());
        statement.setInt(offset + 2, value.widthMillimeters());
        statement.setString(offset + 3, value.alignment());
        statement.setString(offset + 4, value.sellerName());
        statement.setString(offset + 5, value.sellerAddress());
        statement.setString(offset + 6, value.sellerTaxId());
        statement.setString(offset + 7, value.footer());
        statement.setTimestamp(offset + 8, timestamp);
    }

    private static boolean empty(String value) { return value == null || value.trim().isEmpty(); }
    private static String bounded(String value, int maximum, String label) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maximum) throw new IllegalArgumentException(label + " is too long");
        return normalized;
    }

    private static String logoMarkup(InvoiceBrandingDto branding) {
        if (branding == null || !branding.hasLogo()) return "";
        String align = branding.alignment().toLowerCase(java.util.Locale.ROOT);
        return "<div style=\"text-align:" + align + ";margin-bottom:5mm\"><img style=\"width:"
                + branding.widthMillimeters() + "mm;height:auto\" src=\"data:" + branding.mediaType()
                + ";base64," + Base64.getEncoder().encodeToString(branding.logo()) + "\" /></div>";
    }

    private static String resource(String filename) throws IOException {
        return new String(resourceBytes(filename), StandardCharsets.UTF_8);
    }

    private static byte[] resourceBytes(String filename) throws IOException {
        String path = "plugins/community/invoice/" + filename;
        try (InputStream input = JdbcInvoiceDocumentService.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing invoice resource: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }
}
