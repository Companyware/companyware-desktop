package core.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import platform.api.security.AuditEvent;
import platform.api.security.AuditSink;
import platform.api.security.AuthenticationResult;
import platform.api.security.AuthenticationService;
import platform.api.security.AuthorizationService;
import platform.api.security.UserIdentity;
import platform.api.ui.AccessRequirement;

/** Thread-safe authentication session shared by private and company profiles. */
public final class DefaultIdentityService implements AuthenticationService, AuthorizationService {
    private final UserAccountStore accounts;
    private final PasswordHasher passwords;
    private final AuditSink audit;
    private final AtomicReference<UserIdentity> current = new AtomicReference<UserIdentity>();

    public DefaultIdentityService(UserAccountStore accounts, PasswordHasher passwords, AuditSink audit) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.audit = audit;
    }

    @Override public AuthenticationResult authenticate(String username, char[] password) {
        String normalized = username == null ? "" : username.trim();
        try {
            Optional<UserAccount> found = accounts.findActive(normalized);
            if (!found.isPresent() || password == null || !passwords.matches(password, found.get().passwordHash())) {
                audit.record(new AuditEvent("login", normalized, "denied"));
                return AuthenticationResult.invalid();
            }
            UserAccount account = found.get();
            UserIdentity identity = new UserIdentity(account.username(), account.roles(), account.permissions());
            if (account.passwordChangeRequired()) {
                audit.record(new AuditEvent("login", normalized, "credential-change-required"));
                return AuthenticationResult.credentialChangeRequired(identity);
            }
            current.set(identity);
            audit.record(new AuditEvent("login", normalized, "success"));
            return AuthenticationResult.authenticated(identity);
        } finally { clear(password); }
    }

    @Override public AuthenticationResult changePassword(String username, char[] currentPassword, char[] newPassword) {
        String normalized = username == null ? "" : username.trim();
        try {
            Optional<UserAccount> found = accounts.findActive(normalized);
            if (!found.isPresent() || !passwords.matches(currentPassword, found.get().passwordHash())
                    || !PasswordPolicy.accepts(newPassword) || passwords.matches(newPassword, found.get().passwordHash())) {
                audit.record(new AuditEvent("credential-change", normalized, "denied"));
                return AuthenticationResult.invalid();
            }
            accounts.replacePassword(normalized, passwords.hash(newPassword));
            UserAccount account = found.get();
            UserIdentity identity = new UserIdentity(account.username(), account.roles(), account.permissions());
            current.set(identity);
            audit.record(new AuditEvent("credential-change", normalized, "success"));
            return AuthenticationResult.authenticated(identity);
        } finally { clear(currentPassword); clear(newPassword); }
    }

    @Override public void logout() {
        UserIdentity identity = current.getAndSet(null);
        audit.record(new AuditEvent("logout", identity == null ? null : identity.username(), "success"));
    }
    @Override public Optional<UserIdentity> currentIdentity() { return Optional.ofNullable(current.get()); }
    @Override public boolean hasPermission(String permission) {
        UserIdentity identity = current.get();
        return identity != null && identity.hasPermission(permission);
    }
    @Override public boolean permits(AccessRequirement requirement) {
        if (requirement == null || !requirement.requiresAuthentication()) return true;
        UserIdentity identity = current.get();
        return identity != null && (requirement.permission() == null || identity.hasPermission(requirement.permission()));
    }

    private static void clear(char[] value) { if (value != null) Arrays.fill(value, '\0'); }
}
