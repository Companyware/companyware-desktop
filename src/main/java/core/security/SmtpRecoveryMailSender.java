// Companyware
// Copyright (c) 2026 Companyware contributors
// SPDX-License-Identifier: MIT
package core.security;

import java.util.Arrays;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import platform.api.security.SecretProvider;

/** TLS-capable, deployment-configured SMTP transport for account security mail. */
public final class SmtpRecoveryMailSender implements RecoveryMailSender {
    private final SecretProvider secrets;
    public SmtpRecoveryMailSender(SecretProvider secrets) { this.secrets = secrets; }

    @Override public void sendVerification(String recipient, String link) throws Exception {
        send(recipient, "Companyware: E-Mail-Adresse bestätigen",
                "Bestätige deine E-Mail-Adresse über diesen einmalig nutzbaren Link (30 Minuten gültig):\n\n" + link);
    }
    @Override public void sendRecovery(String recipient, String link) throws Exception {
        send(recipient, "Companyware: Zugang wiederherstellen",
                "Setze dein Passwort über diesen einmalig nutzbaren Link zurück (30 Minuten gültig):\n\n" + link
                + "\n\nFalls du dies nicht angefordert hast, ignoriere diese Nachricht.");
    }
    private void send(String recipient, String subject, String body) throws Exception {
        final String host = setting("host", null);
        final int port = Integer.parseInt(setting("port", "587"));
        final String from = setting("from", null);
        final String username = setting("username", "").trim();
        final boolean tls = Boolean.parseBoolean(setting("starttls", "true"));
        if (host == null || from == null) throw new IllegalStateException("Recovery SMTP host/from are not configured");
        final String password = secret(username.length() > 0);
        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", host); properties.setProperty("mail.smtp.port", String.valueOf(port));
        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(tls));
        properties.setProperty("mail.smtp.starttls.required", String.valueOf(tls));
        properties.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        properties.setProperty("mail.smtp.auth", String.valueOf(username.length() > 0));
        properties.setProperty("mail.smtp.connectiontimeout", "10000"); properties.setProperty("mail.smtp.timeout", "30000");
        Session session = Session.getInstance(properties, username.length() == 0 ? null : new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(username, password); }
        });
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from, true));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipient, true));
        message.setSubject(subject, "UTF-8"); message.setText(body, "UTF-8");
        Transport.send(message);
    }
    private String setting(String name, String fallback) {
        String env = System.getenv("CW_RECOVERY_SMTP_" + name.toUpperCase());
        String value = env == null ? System.getProperty("companyware.recovery.smtp." + name) : env;
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
    private String secret(boolean required) {
        java.util.Optional<char[]> found = secrets.find("recovery.smtp.password");
        if (!found.isPresent()) { if (required) throw new IllegalStateException("Recovery SMTP password is not configured"); return null; }
        char[] value = found.get(); try { return new String(value); } finally { Arrays.fill(value, '\0'); }
    }
}
