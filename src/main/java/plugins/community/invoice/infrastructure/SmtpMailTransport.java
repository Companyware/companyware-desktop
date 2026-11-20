package plugins.community.invoice.infrastructure;

import java.util.Properties;
import javax.activation.DataHandler;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import platform.api.security.SecretProvider;
import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import plugins.community.invoice.application.SmtpSettingsDto;

/** TLS-capable SMTP adapter configured outside invoice data and templates. */
final class SmtpMailTransport implements MailTransport {
    private final SecretProvider secrets;
    private final java.util.function.Supplier<SmtpSettingsDto> settings;
    SmtpMailTransport(SecretProvider secrets, java.util.function.Supplier<SmtpSettingsDto> settings) {
        this.secrets = secrets; this.settings = settings;
    }

    @Override public void send(InvoiceMailDraftDto draft, EInvoiceArchiveDto artifact, boolean xml) throws Exception {
        SmtpSettingsDto smtp = settings.get();
        final String username = blank(smtp.username()) ? null : smtp.username().trim();
        final String password = secret("invoice.smtp.password", username != null);
        Session session = session(smtp, username, password);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(senderAddress(smtp));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(draft.recipient(), false));
        message.setSubject(draft.subject(), "UTF-8");
        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart body = new MimeBodyPart(); body.setText(draft.body(), "UTF-8"); multipart.addBodyPart(body);
        multipart.addBodyPart(attachment(artifact.filename(), artifact.pdf(), "application/pdf"));
        if (xml) multipart.addBodyPart(attachment(artifact.filename().replaceFirst("(?i)\\.pdf$", "-xrechnung.xml"), artifact.xml(), "application/xml"));
        message.setContent(multipart); Transport.send(message);
    }
    @Override public void test(SmtpSettingsDto smtp, char[] suppliedPassword) throws Exception {
        validate(smtp);
        final String username = blank(smtp.username()) ? null : smtp.username().trim();
        String password = suppliedPassword == null || suppliedPassword.length == 0
                ? secret("invoice.smtp.password", username != null) : new String(suppliedPassword);
        Session session = session(smtp, username, password);
        Transport connection = session.getTransport("smtp");
        try { connection.connect(smtp.host().trim(), smtp.port(), username, password); }
        finally { if (connection.isConnected()) connection.close(); }
    }
    private static Session session(SmtpSettingsDto smtp, final String username, final String password) {
        validate(smtp);
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", smtp.host().trim()); properties.setProperty("mail.smtp.port", String.valueOf(smtp.port()));
        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(smtp.startTls()));
        properties.setProperty("mail.smtp.starttls.required", String.valueOf(smtp.startTls()));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.connectiontimeout", "10000"); properties.setProperty("mail.smtp.timeout", "30000");
        properties.setProperty("mail.smtp.writetimeout", "30000"); properties.setProperty("mail.smtp.auth", String.valueOf(username != null));
        return Session.getInstance(properties, username == null ? null : new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(username, password); }
        });
    }
    private static void validate(SmtpSettingsDto smtp) {
        if (smtp == null || blank(smtp.host())) throw new IllegalArgumentException("SMTP host is required");
        if (smtp.port() < 1 || smtp.port() > 65535) throw new IllegalArgumentException("SMTP port is invalid");
        if (blank(smtp.from())) throw new IllegalArgumentException("SMTP sender is required");
        try { new InternetAddress(smtp.from(), true).validate(); }
        catch (Exception invalid) { throw new IllegalArgumentException("SMTP sender is invalid"); }
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    static InternetAddress senderAddress(SmtpSettingsDto smtp) throws Exception {
        return blank(smtp.fromName()) ? new InternetAddress(smtp.from())
                : new InternetAddress(smtp.from(), smtp.fromName().trim(), "UTF-8");
    }
    private static MimeBodyPart attachment(String name, byte[] data, String type) throws Exception {
        MimeBodyPart part = new MimeBodyPart(); part.setDataHandler(new DataHandler(new ByteArrayDataSource(data, type))); part.setFileName(name); return part;
    }
    private String secret(String key, boolean required) { java.util.Optional<char[]> found = secrets.find(key); if (!found.isPresent()) { if (required) throw new IllegalStateException("Missing SMTP secret: " + key); return null; } char[] value = found.get(); try { return new String(value); } finally { java.util.Arrays.fill(value, '\0'); } }
}
