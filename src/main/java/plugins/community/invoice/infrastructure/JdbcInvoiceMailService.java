package plugins.community.invoice.infrastructure;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import plugins.community.invoice.application.InvoiceMailService;
import plugins.community.invoice.application.InvoiceMailTemplateDto;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.SmtpSettingsDto;
import platform.api.security.SecretProvider;
import platform.api.security.MutableSecretProvider;

/** Persists controlled mail templates and sends only after an explicit reviewed draft. */
public final class JdbcInvoiceMailService implements InvoiceMailService {
    private static final String DEFAULT_SUBJECT = "Rechnung {{invoice.number}} von {{seller.name}}";
    private static final String DEFAULT_BODY = "Guten Tag {{customer.name}},\n\n" +
            "anbei erhalten Sie unsere Rechnung {{invoice.number}} über {{invoice.gross}} {{invoice.currency}}.\n\n" +
            "Mit freundlichen Grüßen\n{{seller.name}}";
    private static final Pattern TOKEN = Pattern.compile("\\{\\{[^{}]+\\}\\}");
    private final PluginDatabase database;
    private final InvoiceService invoices;
    private final EInvoiceService eInvoices;
    private final MailTransport transport;
    private final SecretProvider secrets;
    private final MutableSecretProvider mutableSecrets;

    public JdbcInvoiceMailService(PluginDatabase database, InvoiceService invoices,
            EInvoiceService eInvoices, SecretProvider secrets, MutableSecretProvider mutableSecrets) {
        this.database = database; this.invoices = invoices; this.eInvoices = eInvoices;
        this.secrets = secrets; this.mutableSecrets = mutableSecrets;
        this.transport = new SmtpMailTransport(secrets, this::loadSettingsOrThrow);
    }
    JdbcInvoiceMailService(PluginDatabase database, InvoiceService invoices,
            EInvoiceService eInvoices, MailTransport transport) {
        this.database = database; this.invoices = invoices; this.eInvoices = eInvoices; this.transport = transport;
        this.secrets = null; this.mutableSecrets = null;
    }

    @Override public ApplicationResult<InvoiceMailTemplateDto> template() {
        try {
            InvoiceMailTemplateDto value = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("SELECT SUBJECT_TEMPLATE,BODY_TEMPLATE FROM CW_INVOICE_MAIL_TEMPLATE WHERE SINGLETON_ID=1"); ResultSet row = statement.executeQuery()) {
                    return row.next() ? new InvoiceMailTemplateDto(row.getString(1), row.getString(2)) : null;
                }
            });
            return ApplicationResult.success(value == null ? defaults() : value);
        } catch (Exception failure) { return unavailable("Mail template could not be loaded"); }
    }

    @Override public ApplicationResult<InvoiceMailTemplateDto> saveTemplate(InvoiceMailTemplateDto value) {
        try {
            validate(value);
            final InvoiceMailTemplateDto normalized = new InvoiceMailTemplateDto(value.subject().trim(), value.body().trim());
            database.write(connection -> {
                try (PreparedStatement update = connection.prepareStatement("UPDATE CW_INVOICE_MAIL_TEMPLATE SET SUBJECT_TEMPLATE=?,BODY_TEMPLATE=?,UPDATED_AT=? WHERE SINGLETON_ID=1")) {
                    bind(update, normalized); if (update.executeUpdate() == 0) try (PreparedStatement insert = connection.prepareStatement("INSERT INTO CW_INVOICE_MAIL_TEMPLATE (SUBJECT_TEMPLATE,BODY_TEMPLATE,UPDATED_AT,SINGLETON_ID) VALUES (?,?,?,1)")) { bind(insert, normalized); insert.executeUpdate(); }
                }
                return null;
            });
            return ApplicationResult.success(normalized);
        } catch (IllegalArgumentException invalid) { return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage()); }
        catch (Exception failure) { return unavailable("Mail template could not be saved"); }
    }

    @Override public ApplicationResult<InvoiceMailTemplateDto> restoreDefaultTemplate() { return saveTemplate(defaults()); }

    @Override public ApplicationResult<InvoiceMailDraftDto> draft(long invoiceId) {
        ApplicationResult<InvoiceDetailsDto> details = invoices.details(invoiceId);
        if (!details.isSuccess()) return ApplicationResult.failure(details.errorCode().get(), details.message());
        if (!"FINALIZED".equals(details.valueOrThrow().summary().status()))
            return ApplicationResult.failure(ApplicationErrorCode.CONFLICT, "Only a finalized invoice can be sent");
        ApplicationResult<InvoiceMailTemplateDto> template = template();
        if (!template.isSuccess()) return ApplicationResult.failure(template.errorCode().get(), template.message());
        InvoiceDetailsDto invoice = details.valueOrThrow();
        return ApplicationResult.success(new InvoiceMailDraftDto(invoiceId, "",
                render(template.valueOrThrow().subject(), invoice), render(template.valueOrThrow().body(), invoice)));
    }

    @Override public ApplicationResult<Void> send(InvoiceMailDraftDto draft, boolean attachXml) {
        try {
            validateDraft(draft);
            ApplicationResult<InvoiceDetailsDto> details = invoices.details(draft.invoiceId());
            if (!details.isSuccess() || !"FINALIZED".equals(details.valueOrThrow().summary().status()))
                return ApplicationResult.failure(ApplicationErrorCode.CONFLICT, "Only a finalized invoice can be sent");
            ApplicationResult<EInvoiceArchiveDto> archived = eInvoices.archive(draft.invoiceId());
            if (!archived.isSuccess()) return ApplicationResult.failure(archived.errorCode().get(), archived.message());
            transport.send(draft, archived.valueOrThrow(), attachXml);
            database.write(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO CW_INVOICE_MAIL_LOG (INVOICE_ID,RECIPIENT,SUBJECT,SENT_AT,ATTACHED_XML) VALUES (?,?,?,?,?)")) {
                    statement.setLong(1, draft.invoiceId()); statement.setString(2, draft.recipient().trim()); statement.setString(3, draft.subject().trim()); statement.setTimestamp(4, Timestamp.from(Instant.now())); statement.setInt(5, attachXml ? 1 : 0); statement.executeUpdate();
                }
                return null;
            });
            return ApplicationResult.success(null);
        } catch (IllegalArgumentException invalid) { return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage()); }
        catch (Exception failure) { return unavailable("Invoice mail could not be sent: " + safeMessage(failure)); }
    }

    @Override public ApplicationResult<SmtpSettingsDto> smtpSettings() {
        try { return ApplicationResult.success(loadSettingsOrThrow()); }
        catch (Exception failure) { return unavailable("SMTP settings could not be loaded"); }
    }

    @Override public ApplicationResult<SmtpSettingsDto> saveSmtpSettings(SmtpSettingsDto value, char[] password) {
        try {
            validateSettings(value);
            final SmtpSettingsDto normalized = normalized(value, passwordAvailable(password));
            database.write(connection -> {
                try (PreparedStatement update = connection.prepareStatement("UPDATE CW_INVOICE_SMTP_SETTINGS SET SMTP_HOST=?,SMTP_PORT=?,SMTP_FROM_NAME=?,SMTP_FROM=?,SMTP_USERNAME=?,START_TLS=?,UPDATED_AT=? WHERE SINGLETON_ID=1")) {
                    bind(update, normalized); if (update.executeUpdate() == 0) try (PreparedStatement insert = connection.prepareStatement("INSERT INTO CW_INVOICE_SMTP_SETTINGS (SMTP_HOST,SMTP_PORT,SMTP_FROM_NAME,SMTP_FROM,SMTP_USERNAME,START_TLS,UPDATED_AT,SINGLETON_ID) VALUES (?,?,?,?,?,?,?,1)")) { bind(insert, normalized); insert.executeUpdate(); }
                }
                return null;
            });
            if (password != null && password.length > 0) {
                if (mutableSecrets == null) throw new IllegalStateException("Session secret storage is unavailable");
                mutableSecrets.setForSession("invoice.smtp.password", password);
            }
            return ApplicationResult.success(loadSettingsOrThrow());
        } catch (IllegalArgumentException invalid) { return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage()); }
        catch (Exception failure) { return unavailable("SMTP settings could not be saved: " + safeMessage(failure)); }
        finally { if (password != null) java.util.Arrays.fill(password, '\0'); }
    }

    @Override public ApplicationResult<Void> testSmtpSettings(SmtpSettingsDto value, char[] password) {
        try { validateSettings(value); transport.test(normalized(value, passwordAvailable(password)), password); return ApplicationResult.success(null); }
        catch (IllegalArgumentException invalid) { return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, invalid.getMessage()); }
        catch (Exception failure) { return unavailable("SMTP connection failed: " + safeMessage(failure)); }
        finally { if (password != null) java.util.Arrays.fill(password, '\0'); }
    }

    private SmtpSettingsDto loadSettingsOrThrow() {
        try {
            SmtpSettingsDto stored = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("SELECT SMTP_HOST,SMTP_PORT,SMTP_FROM_NAME,SMTP_FROM,SMTP_USERNAME,START_TLS FROM CW_INVOICE_SMTP_SETTINGS WHERE SINGLETON_ID=1"); ResultSet row = statement.executeQuery()) {
                    return row.next() ? new SmtpSettingsDto(row.getString(1), row.getInt(2), row.getString(3), row.getString(4), row.getString(5), row.getInt(6) != 0, false) : null;
                }
            });
            String host = stored == null ? System.getProperty("companyware.invoice.smtp.host", "") : stored.host();
            int port = stored == null ? integerProperty("companyware.invoice.smtp.port", 587) : stored.port();
            String fromName = stored == null ? System.getProperty("companyware.invoice.smtp.fromName", "") : stored.fromName();
            String from = stored == null ? System.getProperty("companyware.invoice.smtp.from", "") : stored.from();
            String username = stored == null ? secretString("invoice.smtp.username") : stored.username();
            boolean tls = stored == null ? Boolean.parseBoolean(System.getProperty("companyware.invoice.smtp.starttls", "true")) : stored.startTls();
            return new SmtpSettingsDto(host, port, fromName, from, username, tls, secretPresent("invoice.smtp.password"));
        } catch (RuntimeException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    private boolean secretPresent(String name) { return secrets != null && secrets.find(name).isPresent(); }
    private String secretString(String name) { if (secrets == null) return ""; java.util.Optional<char[]> found = secrets.find(name); if (!found.isPresent()) return ""; char[] value = found.get(); try { return new String(value); } finally { java.util.Arrays.fill(value, '\0'); } }
    private static int integerProperty(String name, int fallback) { try { return Integer.parseInt(System.getProperty(name, String.valueOf(fallback))); } catch (NumberFormatException invalid) { return fallback; } }
    private boolean passwordAvailable(char[] password) { return (password != null && password.length > 0) || secretPresent("invoice.smtp.password"); }
    private static SmtpSettingsDto normalized(SmtpSettingsDto value, boolean passwordAvailable) { return new SmtpSettingsDto(value.host().trim(), value.port(), value.fromName() == null ? "" : value.fromName().trim(), value.from().trim(), value.username() == null ? "" : value.username().trim(), value.startTls(), passwordAvailable); }
    private static void validateSettings(SmtpSettingsDto value) {
        if (value == null || value.host() == null || value.host().trim().isEmpty() || value.host().length() > 255) throw new IllegalArgumentException("SMTP host is required");
        if (value.port() < 1 || value.port() > 65535) throw new IllegalArgumentException("SMTP port is invalid");
        if (value.fromName() != null && value.fromName().length() > 200) throw new IllegalArgumentException("SMTP sender name is invalid");
        try { javax.mail.internet.InternetAddress address = new javax.mail.internet.InternetAddress(value.from(), true); address.validate(); } catch (Exception invalid) { throw new IllegalArgumentException("SMTP sender is invalid"); }
        if (value.username() != null && value.username().length() > 320) throw new IllegalArgumentException("SMTP username is invalid");
    }
    private static void bind(PreparedStatement statement, SmtpSettingsDto value) throws Exception { statement.setString(1, value.host()); statement.setInt(2, value.port()); statement.setString(3, value.fromName()); statement.setString(4, value.from()); statement.setString(5, value.username()); statement.setInt(6, value.startTls() ? 1 : 0); statement.setTimestamp(7, Timestamp.from(Instant.now())); }

    private static void bind(PreparedStatement statement, InvoiceMailTemplateDto value) throws Exception { statement.setString(1, value.subject()); statement.setString(2, value.body()); statement.setTimestamp(3, Timestamp.from(Instant.now())); }
    private static InvoiceMailTemplateDto defaults() { return new InvoiceMailTemplateDto(DEFAULT_SUBJECT, DEFAULT_BODY); }
    private static void validate(InvoiceMailTemplateDto value) {
        if (value == null || value.subject() == null || value.subject().trim().isEmpty() || value.subject().length() > 500) throw new IllegalArgumentException("Mail subject is required and may contain at most 500 characters");
        if (value.body() == null || value.body().trim().isEmpty() || value.body().length() > 10000) throw new IllegalArgumentException("Mail body is required and may contain at most 10000 characters");
        validateTokens(value.subject()); validateTokens(value.body());
    }
    private static void validateTokens(String value) { Matcher matcher = TOKEN.matcher(value); while (matcher.find()) if (!isKnown(matcher.group())) throw new IllegalArgumentException("Unknown mail-template token: " + matcher.group()); }
    private static boolean isKnown(String token) { return "{{invoice.number}}".equals(token) || "{{invoice.gross}}".equals(token) || "{{invoice.currency}}".equals(token) || "{{customer.name}}".equals(token) || "{{seller.name}}".equals(token); }
    private static String render(String value, InvoiceDetailsDto invoice) { return value.replace("{{invoice.number}}", invoice.summary().invoiceNumber()).replace("{{invoice.gross}}", invoice.summary().gross().toPlainString()).replace("{{invoice.currency}}", invoice.currency()).replace("{{customer.name}}", invoice.summary().customerName()).replace("{{seller.name}}", invoice.sellerName()); }
    private static void validateDraft(InvoiceMailDraftDto draft) { if (draft == null) throw new IllegalArgumentException("Mail draft is required"); try { javax.mail.internet.InternetAddress address = new javax.mail.internet.InternetAddress(draft.recipient(), true); address.validate(); } catch (Exception invalid) { throw new IllegalArgumentException("A valid recipient e-mail address is required"); } if (draft.subject() == null || draft.subject().trim().isEmpty() || draft.subject().length() > 500) throw new IllegalArgumentException("Mail subject is invalid"); if (draft.body() == null || draft.body().trim().isEmpty() || draft.body().length() > 10000) throw new IllegalArgumentException("Mail body is invalid"); }
    private static <T> ApplicationResult<T> unavailable(String message) { return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, message); }
    private static String safeMessage(Exception failure) { String value = failure.getMessage(); return value == null || value.trim().isEmpty() ? failure.getClass().getSimpleName() : value; }
}
