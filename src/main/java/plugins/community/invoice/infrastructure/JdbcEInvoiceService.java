package plugins.community.invoice.infrastructure;

import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoicePdfDto;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.CreateInvoiceCommand;
import plugins.community.invoice.application.InvoiceLineInput;

/** Derby-backed immutable e-invoice archive and correction workflow. */
public final class JdbcEInvoiceService implements EInvoiceService {
    private final PluginDatabase database;
    private final InvoiceService invoices;
    private final InvoiceDocumentService documents;
    private final EInvoiceXmlGenerator generator = new EInvoiceXmlGenerator();
    private final EInvoiceValidator validator = new EInvoiceValidator();
    private final PdfA3InvoiceEmbedder embedder = new PdfA3InvoiceEmbedder();

    public JdbcEInvoiceService(PluginDatabase database, InvoiceService invoices, InvoiceDocumentService documents) {
        this.database = database; this.invoices = invoices; this.documents = documents;
    }

    @Override public ApplicationResult<EInvoiceArchiveDto> archive(long invoiceId) {
        try {
            ApplicationResult<EInvoiceArchiveDto> existing = archived(invoiceId);
            if (existing.isSuccess()) return existing;
            if (existing.errorCode().get() != ApplicationErrorCode.NOT_FOUND)
                return ApplicationResult.failure(existing.errorCode().get(), existing.message());
            ApplicationResult<InvoiceDetailsDto> details = invoices.details(invoiceId);
            if (!details.isSuccess()) return ApplicationResult.failure(details.errorCode().get(), details.message());
            InvoiceDetailsDto invoice = details.valueOrThrow();
            if (!"FINALIZED".equals(invoice.summary().status()))
                return ApplicationResult.failure(ApplicationErrorCode.CONFLICT, "Only a finalized invoice can be archived");
            Correction correction = correction(invoiceId);
            byte[] ubl = generator.xrechnung(invoice, correction.credit, correction.originalNumber);
            byte[] cii = generator.zugferd(invoice, correction.credit, correction.originalNumber);
            validator.validate(ubl, invoice.summary().invoiceNumber());
            validator.validate(cii, invoice.summary().invoiceNumber());
            ApplicationResult<InvoicePdfDto> pdf = documents.createPdf(invoiceId);
            if (!pdf.isSuccess()) return ApplicationResult.failure(pdf.errorCode().get(), pdf.message());
            byte[] hybrid = embedder.embed(pdf.valueOrThrow().content(), cii);
            String hash = hex(MessageDigest.getInstance("SHA-256").digest(hybrid));
            final Timestamp now = Timestamp.from(Instant.now());
            final String filename = pdf.valueOrThrow().filename();
            database.write(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO CW_EINVOICE_ARCHIVE (INVOICE_ID,FILE_NAME,PDF_DATA,XML_DATA,SHA256,ARCHIVED_AT) VALUES (?,?,?,?,?,?)")) {
                    statement.setLong(1, invoiceId); statement.setString(2, filename); statement.setBytes(3, hybrid);
                    statement.setBytes(4, ubl); statement.setString(5, hash); statement.setTimestamp(6, now);
                    statement.executeUpdate();
                }
                return null;
            });
            return ApplicationResult.success(new EInvoiceArchiveDto(invoiceId, filename, hybrid, ubl, hash, now.toLocalDateTime()));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "E-invoice could not be generated and archived");
        }
    }

    @Override public ApplicationResult<EInvoiceArchiveDto> archived(long invoiceId) {
        try {
            EInvoiceArchiveDto value = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("SELECT FILE_NAME,PDF_DATA,XML_DATA,SHA256,ARCHIVED_AT FROM CW_EINVOICE_ARCHIVE WHERE INVOICE_ID=?")) {
                    statement.setLong(1, invoiceId);
                    try (ResultSet row = statement.executeQuery()) {
                        if (!row.next()) return null;
                        return new EInvoiceArchiveDto(invoiceId, row.getString(1), row.getBytes(2), row.getBytes(3), row.getString(4), row.getTimestamp(5).toLocalDateTime());
                    }
                }
            });
            if (value != null && !value.sha256().equals(hex(MessageDigest.getInstance("SHA-256").digest(value.pdf()))))
                return ApplicationResult.failure(ApplicationErrorCode.CONFLICT, "Archived e-invoice failed its integrity check");
            if (value == null) return ApplicationResult.failure(ApplicationErrorCode.NOT_FOUND, "E-invoice has not been archived");
            return ApplicationResult.success(value);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "E-invoice archive is unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceDetailsDto> createCorrectionDraft(long originalInvoiceId, String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isEmpty() || normalized.length() > 1000)
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, "A correction reason is required");
        ApplicationResult<EInvoiceArchiveDto> archived = archive(originalInvoiceId);
        if (!archived.isSuccess()) return ApplicationResult.failure(archived.errorCode().get(), archived.message());
        try {
            InvoiceDetailsDto original = invoices.details(originalInvoiceId).valueOrThrow();
            java.util.List<InvoiceLineInput> lines = new java.util.ArrayList<InvoiceLineInput>();
            for (plugins.community.invoice.application.InvoiceLineDto line : original.lines())
                lines.add(new InvoiceLineInput(line.description(), line.quantity(), line.unitNet(), line.taxRate()));
            CreateInvoiceCommand command = new CreateInvoiceCommand(original.sellerName(), original.sellerAddress(),
                    original.sellerTaxId(), original.summary().customerName(), original.customerAddress(),
                    original.serviceDate(), original.currency(), original.paymentTerms(), lines);
            long correctionId = invoices.createDraft(command).valueOrThrow().id();
            database.write(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO CW_INVOICE_CORRECTION (ORIGINAL_INVOICE_ID,CORRECTION_INVOICE_ID,REASON,CREATED_AT) VALUES (?,?,?,?)")) {
                    statement.setLong(1, originalInvoiceId); statement.setLong(2, correctionId); statement.setString(3, normalized); statement.setTimestamp(4, Timestamp.from(Instant.now())); statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("UPDATE CW_INVOICE SET STATUS='CANCELLED' WHERE ID=? AND STATUS='FINALIZED'")) { statement.setLong(1, originalInvoiceId); statement.executeUpdate(); }
                return null;
            });
            return invoices.details(correctionId);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.CONFLICT, "Correction draft could not be created");
        }
    }

    private Correction correction(long invoiceId) throws Exception {
        return database.read(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT I.INVOICE_NUMBER FROM CW_INVOICE_CORRECTION C JOIN CW_INVOICE I ON I.ID=C.ORIGINAL_INVOICE_ID WHERE C.CORRECTION_INVOICE_ID=?")) {
                statement.setLong(1, invoiceId); try (ResultSet row = statement.executeQuery()) { return row.next() ? new Correction(true, row.getString(1)) : new Correction(false, null); }
            }
        });
    }
    private static String hex(byte[] bytes) { StringBuilder value = new StringBuilder(); for (byte b : bytes) value.append(String.format("%02x", b & 0xff)); return value.toString(); }
    private static final class Correction { final boolean credit; final String originalNumber; Correction(boolean credit, String originalNumber) { this.credit = credit; this.originalNumber = originalNumber; } }
}
