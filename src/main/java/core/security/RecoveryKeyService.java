// Companyware
// Copyright (c) 2026 Companyware contributors
//
// SPDX-License-Identifier: MIT
// Licensed under the MIT License; see LICENSE in the project root.
// This file is part of Companyware.
package core.security;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Base64;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import core.HibernateUtils;
import platform.api.security.AuditEvent;
import platform.api.security.AuditSink;
import platform.api.security.AccountRecoveryService;
import platform.api.security.RecoveryResult;

/** Issues and atomically consumes high-entropy, one-time account recovery keys. */
public final class RecoveryKeyService implements AccountRecoveryService {
    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 15L * 60L * 1000L;
    private final PasswordHasher hashes;
    private final AuditSink audit;
    private final RecoveryMailSender mail;
    private final SecureRandom random = new SecureRandom();

    public RecoveryKeyService(PasswordHasher hashes, AuditSink audit) {
        this(hashes, audit, null);
    }

    public RecoveryKeyService(PasswordHasher hashes, AuditSink audit, RecoveryMailSender mail) {
        this.hashes = hashes; this.audit = audit; this.mail = mail;
    }

    public String rotate(String username) {
        byte[] selectorBytes = new byte[16];
        byte[] secretBytes = new byte[24];
        random.nextBytes(selectorBytes);
        random.nextBytes(secretBytes);
        String selector = Base64.getUrlEncoder().withoutPadding().encodeToString(selectorBytes);
        char[] secret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes).toCharArray();
        String hash = hashes.hash(secret);
        ConnectionHandle handle = open();
        try {
            handle.connection.setAutoCommit(false);
            try (PreparedStatement account = handle.connection.prepareStatement("SELECT active FROM Users WHERE username=? FOR UPDATE")) {
                account.setString(1, username);
                try (ResultSet result = account.executeQuery()) {
                    if (!result.next() || !result.getBoolean(1)) throw new IllegalArgumentException("Active account required");
                }
            }
            try (PreparedStatement revoke = handle.connection.prepareStatement(
                    "UPDATE AccountRecoveryKeys SET active=FALSE WHERE username=? AND active=TRUE")) {
                revoke.setString(1, username); revoke.executeUpdate();
            }
            try (PreparedStatement insert = handle.connection.prepareStatement(
                    "INSERT INTO AccountRecoveryKeys (selector,username,secret_hash,active,failures,issued_at) VALUES (?,?,?,TRUE,0,?)")) {
                insert.setString(1, selector); insert.setString(2, username); insert.setString(3, hash);
                insert.setTimestamp(4, new Timestamp(System.currentTimeMillis())); insert.executeUpdate();
            }
            handle.connection.commit();
            audit("recovery-key-rotate", username, "success");
            return selector + "." + new String(secret);
        } catch (SQLException failure) {
            rollback(handle.connection); audit("recovery-key-rotate", username, "failed");
            throw new IllegalStateException("Unable to rotate recovery key", failure);
        } catch (RuntimeException failure) {
            rollback(handle.connection); audit("recovery-key-rotate", username, "denied"); throw failure;
        } finally { Arrays.fill(secret, '\0'); handle.close(); }
    }

    public RecoveryResult reset(String key, char[] newPassword) {
        if (key != null && (key.trim().startsWith("r.") || key.contains("token=r.")))
            return resetEmail(extractToken(key), newPassword);
        String selector = ""; char[] secret = null;
        try {
            int separator = key == null ? -1 : key.indexOf('.');
            if (separator <= 0 || separator == key.length() - 1 || !PasswordPolicy.accepts(newPassword)) {
                audit("account-recovery", null, "denied"); return RecoveryResult.denied();
            }
            selector = key.substring(0, separator);
            secret = key.substring(separator + 1).toCharArray();
            return consume(selector, secret, newPassword);
        } finally { if (secret != null) Arrays.fill(secret, '\0'); if (newPassword != null) Arrays.fill(newPassword, '\0'); }
    }

    @Override public void requestEmailVerification(String username) {
        issueAndSend(username, null, "VERIFY");
    }

    @Override public boolean emailVerified(String username) {
        ConnectionHandle handle = open();
        try (PreparedStatement query = handle.connection.prepareStatement(
                "SELECT email_verified FROM Users WHERE username=? AND active=TRUE")) {
            query.setString(1, username);
            try (ResultSet result = query.executeQuery()) { return result.next() && result.getBoolean(1); }
        } catch (SQLException failure) { return false; }
        finally { handle.close(); }
    }

    @Override public void requestEmailRecovery(String email) {
        String normalized = normalize(email);
        if (normalized == null) { audit("email-recovery-request", null, "accepted"); return; }
        ConnectionHandle handle = open(); String username = null;
        try (PreparedStatement query = handle.connection.prepareStatement(
                "SELECT username FROM Users WHERE LOWER(email)=? AND email_verified=TRUE AND active=TRUE")) {
            query.setString(1, normalized);
            try (ResultSet result = query.executeQuery()) {
                if (result.next()) { username = result.getString(1); if (result.next()) username = null; }
            }
        } catch (SQLException ignored) { }
        finally { handle.close(); }
        if (username != null) issueAndSend(username, normalized, "RECOVER");
        audit("email-recovery-request", null, "accepted");
    }

    @Override public boolean verifyEmail(String verificationLinkOrToken) {
        return consumeVerification(extractToken(verificationLinkOrToken));
    }

    private void issueAndSend(String username, String requestedEmail, String purpose) {
        if (mail == null) { audit("email-token-issue", null, "mail-unavailable"); return; }
        ConnectionHandle handle = open(); char[] secret = null; String selector = null; String email = null;
        try {
            handle.connection.setAutoCommit(false);
            try (PreparedStatement account = handle.connection.prepareStatement(
                    "SELECT email,active FROM Users WHERE username=? FOR UPDATE")) {
                account.setString(1, username);
                try (ResultSet result = account.executeQuery()) {
                    if (!result.next() || !result.getBoolean(2)) { handle.connection.rollback(); return; }
                    email = normalize(result.getString(1));
                }
            }
            if (email == null || (requestedEmail != null && !email.equals(requestedEmail))) { handle.connection.rollback(); return; }
            if ("VERIFY".equals(purpose)) {
                try (PreparedStatement duplicate = handle.connection.prepareStatement(
                        "SELECT COUNT(*) FROM Users WHERE LOWER(email)=? AND username<>?")) {
                    duplicate.setString(1, email); duplicate.setString(2, username);
                    try (ResultSet result = duplicate.executeQuery()) { result.next(); if (result.getInt(1) > 0) { handle.connection.rollback(); return; } }
                }
            }
            selector = randomPart(16); secret = randomPart(24).toCharArray();
            try (PreparedStatement revoke = handle.connection.prepareStatement(
                    "UPDATE AccountEmailTokens SET active=FALSE WHERE username=? AND purpose=? AND active=TRUE")) {
                revoke.setString(1, username); revoke.setString(2, purpose); revoke.executeUpdate();
            }
            try (PreparedStatement insert = handle.connection.prepareStatement(
                    "INSERT INTO AccountEmailTokens (selector,purpose,username,email,secret_hash,expires_at,active,failures,issued_at) VALUES (?,?,?,?,?,?,TRUE,0,?)")) {
                long now = System.currentTimeMillis();
                insert.setString(1, selector); insert.setString(2, purpose); insert.setString(3, username);
                insert.setString(4, email); insert.setString(5, hashes.hash(secret));
                insert.setTimestamp(6, new Timestamp(now + 30L * 60L * 1000L)); insert.setTimestamp(7, new Timestamp(now)); insert.executeUpdate();
            }
            handle.connection.commit();
            String token = ("VERIFY".equals(purpose) ? "v." : "r.") + selector + "." + new String(secret);
            String prefix = linkPrefix(); String link = prefix + (prefix.contains("?") ? "&" : "?") + "token=" + token;
            if ("VERIFY".equals(purpose)) mail.sendVerification(email, link); else mail.sendRecovery(email, link);
            audit("email-token-issue", username, "sent:" + purpose.toLowerCase());
        } catch (Exception failure) {
            rollback(handle.connection);
            if (selector != null) deactivate(selector);
            audit("email-token-issue", null, "failed");
        } finally { if (secret != null) Arrays.fill(secret, '\0'); handle.close(); }
    }

    private boolean consumeVerification(String token) {
        ParsedToken parsed = parse(token, "v."); if (parsed == null) return false;
        ConnectionHandle handle = open();
        try {
            handle.connection.setAutoCommit(false);
            TokenRow row = token(handle.connection, parsed.selector, "VERIFY");
            if (row == null || !valid(handle.connection, row, parsed.secret)) { handle.connection.commit(); return false; }
            try (PreparedStatement duplicate = handle.connection.prepareStatement(
                    "SELECT COUNT(*) FROM Users WHERE LOWER(email)=? AND username<>?")) {
                duplicate.setString(1, normalize(row.email)); duplicate.setString(2, row.username);
                try (ResultSet result = duplicate.executeQuery()) { result.next(); if (result.getInt(1) > 0) { handle.connection.rollback(); return false; } }
            }
            try (PreparedStatement update = handle.connection.prepareStatement(
                    "UPDATE Users SET email_verified=TRUE,email_verified_at=? WHERE username=? AND LOWER(email)=? AND active=TRUE")) {
                update.setTimestamp(1, new Timestamp(System.currentTimeMillis())); update.setString(2, row.username);
                update.setString(3, normalize(row.email)); if (update.executeUpdate() != 1) { handle.connection.rollback(); return false; }
            }
            consume(handle.connection, parsed.selector); handle.connection.commit(); audit("email-verification", row.username, "success"); return true;
        } catch (SQLException failure) { rollback(handle.connection); return false; }
        finally { Arrays.fill(parsed.secret, '\0'); handle.close(); }
    }

    private RecoveryResult resetEmail(String token, char[] newPassword) {
        ParsedToken parsed = parse(token, "r.");
        try {
            if (parsed == null || !PasswordPolicy.accepts(newPassword)) return RecoveryResult.denied();
            ConnectionHandle handle = open();
            try {
                handle.connection.setAutoCommit(false); TokenRow row = token(handle.connection, parsed.selector, "RECOVER");
                if (row == null || !valid(handle.connection, row, parsed.secret)) { handle.connection.commit(); return RecoveryResult.denied(); }
                try (PreparedStatement password = handle.connection.prepareStatement(
                        "UPDATE Users SET password=?,password_change_required=FALSE WHERE username=? AND active=TRUE")) {
                    password.setString(1, hashes.hash(newPassword)); password.setString(2, row.username);
                    if (password.executeUpdate() != 1) { handle.connection.rollback(); return RecoveryResult.denied(); }
                }
                consume(handle.connection, parsed.selector); handle.connection.commit();
                audit("email-account-recovery", row.username, "success"); return RecoveryResult.success(row.username);
            } catch (SQLException failure) { rollback(handle.connection); return RecoveryResult.denied(); }
            finally { handle.close(); }
        } finally { if (parsed != null) Arrays.fill(parsed.secret, '\0'); if (newPassword != null) Arrays.fill(newPassword, '\0'); }
    }

    private TokenRow token(Connection connection, String selector, String purpose) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT username,email,secret_hash,expires_at,failures,locked_until FROM AccountEmailTokens WHERE selector=? AND purpose=? AND active=TRUE FOR UPDATE")) {
            query.setString(1, selector); query.setString(2, purpose);
            try (ResultSet result = query.executeQuery()) { return result.next() ? new TokenRow(selector, result) : null; }
        }
    }
    private boolean valid(Connection connection, TokenRow row, char[] secret) throws SQLException {
        long now = System.currentTimeMillis();
        if (row.expires.getTime() <= now || (row.locked != null && row.locked.getTime() > now)) return false;
        if (hashes.matches(secret, row.hash)) return true;
        int failures = row.failures + 1;
        try (PreparedStatement update = connection.prepareStatement("UPDATE AccountEmailTokens SET failures=?,locked_until=? WHERE selector=?")) {
            update.setInt(1, failures); if (failures >= MAX_FAILURES) update.setTimestamp(2, new Timestamp(now + LOCK_MILLIS)); else update.setNull(2, java.sql.Types.TIMESTAMP);
            update.setString(3, row.selector); update.executeUpdate();
        }
        return false;
    }
    private static void consume(Connection connection, String selector) throws SQLException {
        try (PreparedStatement update = connection.prepareStatement("UPDATE AccountEmailTokens SET active=FALSE,consumed_at=? WHERE selector=? AND active=TRUE")) {
            update.setTimestamp(1, new Timestamp(System.currentTimeMillis())); update.setString(2, selector);
            if (update.executeUpdate() != 1) throw new SQLException("Token changed during consumption");
        }
    }
    private void deactivate(String selector) { ConnectionHandle h = open(); try (PreparedStatement p = h.connection.prepareStatement("UPDATE AccountEmailTokens SET active=FALSE WHERE selector=?")) { p.setString(1, selector); p.executeUpdate(); } catch (SQLException ignored) { } finally { h.close(); } }
    private String randomPart(int bytes) { byte[] value = new byte[bytes]; random.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private static String normalize(String email) { if (email == null) return null; String v = email.trim().toLowerCase(java.util.Locale.ROOT); return v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? v : null; }
    private static String extractToken(String value) { if (value == null) return ""; String v = value.trim(); int at = v.indexOf("token="); if (at >= 0) v = v.substring(at + 6); int amp = v.indexOf('&'); return amp < 0 ? v : v.substring(0, amp); }
    private static ParsedToken parse(String token, String prefix) { String v = extractToken(token); if (!v.startsWith(prefix)) return null; String[] parts = v.substring(prefix.length()).split("\\.", 2); return parts.length == 2 ? new ParsedToken(parts[0], parts[1].toCharArray()) : null; }
    private static String linkPrefix() { String v = System.getenv("CW_RECOVERY_LINK_BASE"); if (v == null) v = System.getProperty("companyware.recovery.link-base", "companyware://account-recovery"); return v; }
    private static final class ParsedToken { final String selector; final char[] secret; ParsedToken(String s, char[] v) { selector=s; secret=v; } }
    private static final class TokenRow { final String selector,username,email,hash; final Timestamp expires,locked; final int failures; TokenRow(String selector, ResultSet r) throws SQLException { this.selector=selector; username=r.getString(1); email=r.getString(2); hash=r.getString(3); expires=r.getTimestamp(4); failures=r.getInt(5); locked=r.getTimestamp(6); } }

    private RecoveryResult consume(String selector, char[] secret, char[] newPassword) {
        ConnectionHandle handle = open();
        try {
            handle.connection.setAutoCommit(false);
            String username; String hash; int failures; Timestamp locked;
            try (PreparedStatement query = handle.connection.prepareStatement(
                    "SELECT username,secret_hash,failures,locked_until FROM AccountRecoveryKeys WHERE selector=? AND active=TRUE FOR UPDATE")) {
                query.setString(1, selector);
                try (ResultSet result = query.executeQuery()) {
                    if (!result.next()) { handle.connection.rollback(); audit("account-recovery", null, "denied"); return RecoveryResult.denied(); }
                    username = result.getString(1); hash = result.getString(2); failures = result.getInt(3); locked = result.getTimestamp(4);
                }
            }
            long now = System.currentTimeMillis();
            if (locked != null && locked.getTime() > now) { handle.connection.rollback(); audit("account-recovery", null, "denied"); return RecoveryResult.denied(); }
            if (!hashes.matches(secret, hash)) {
                failures++;
                try (PreparedStatement update = handle.connection.prepareStatement(
                        "UPDATE AccountRecoveryKeys SET failures=?,locked_until=? WHERE selector=?")) {
                    update.setInt(1, failures);
                    if (failures >= MAX_FAILURES) update.setTimestamp(2, new Timestamp(now + LOCK_MILLIS)); else update.setNull(2, java.sql.Types.TIMESTAMP);
                    update.setString(3, selector); update.executeUpdate();
                }
                handle.connection.commit(); audit("account-recovery", null, "denied"); return RecoveryResult.denied();
            }
            boolean active;
            try (PreparedStatement user = handle.connection.prepareStatement("SELECT active FROM Users WHERE username=? FOR UPDATE")) {
                user.setString(1, username);
                try (ResultSet result = user.executeQuery()) { active = result.next() && result.getBoolean(1); }
            }
            if (!active) { handle.connection.rollback(); audit("account-recovery", null, "denied"); return RecoveryResult.denied(); }
            try (PreparedStatement password = handle.connection.prepareStatement(
                    "UPDATE Users SET password=?,password_change_required=FALSE WHERE username=? AND active=TRUE")) {
                password.setString(1, hashes.hash(newPassword)); password.setString(2, username);
                if (password.executeUpdate() != 1) throw new SQLException("Account changed during recovery");
            }
            try (PreparedStatement consume = handle.connection.prepareStatement(
                    "UPDATE AccountRecoveryKeys SET active=FALSE,consumed_at=? WHERE selector=? AND active=TRUE")) {
                consume.setTimestamp(1, new Timestamp(now)); consume.setString(2, selector);
                if (consume.executeUpdate() != 1) throw new SQLException("Recovery key changed during recovery");
            }
            handle.connection.commit(); audit("account-recovery", username, "success"); return RecoveryResult.success(username);
        } catch (SQLException failure) {
            rollback(handle.connection); audit("account-recovery", null, "failed"); return RecoveryResult.denied();
        } finally { handle.close(); }
    }

    private ConnectionHandle open() {
        try {
            SessionFactory factory = HibernateUtils.getSessionFactory();
            ConnectionProvider provider = factory.getSessionFactoryOptions().getServiceRegistry().getService(ConnectionProvider.class);
            return new ConnectionHandle(provider, provider.getConnection());
        } catch (SQLException failure) { throw new IllegalStateException("Recovery database unavailable", failure); }
    }
    private void audit(String action, String actor, String outcome) { if (audit != null) audit.record(new AuditEvent(action, actor, outcome)); }
    private static void rollback(Connection connection) { try { connection.rollback(); } catch (SQLException ignored) { } }

    private static final class ConnectionHandle {
        private final ConnectionProvider provider; private final Connection connection;
        private ConnectionHandle(ConnectionProvider provider, Connection connection) { this.provider = provider; this.connection = connection; }
        private void close() { try { connection.setAutoCommit(true); } catch (SQLException ignored) { } try { provider.closeConnection(connection); } catch (SQLException ignored) { } }
    }
}
