package core.security;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import platform.api.security.AuditEvent;
import platform.api.security.AuthenticationResult;
import platform.api.ui.AccessRequirement;

public class DefaultIdentityServiceTest {
    @Test public void forcesInitialChangeThenAuthorizesPermissions() {
        MemoryStore store = new MemoryStore(true);
        Events audit = new Events();
        DefaultIdentityService service = new DefaultIdentityService(store, new PlainHasher(), audit);
        char[] initial = "companyware".toCharArray();

        AuthenticationResult login = service.authenticate("admin", initial);

        assertEquals(AuthenticationResult.Status.CREDENTIAL_CHANGE_REQUIRED, login.status());
        assertFalse(service.currentIdentity().isPresent());
        assertCleared(initial);

        AuthenticationResult changed = service.changePassword("admin", "companyware".toCharArray(), "new-password".toCharArray());
        assertTrue(changed.isAuthenticated());
        assertTrue(service.permits(AccessRequirement.AUTHENTICATED));
        assertTrue(service.permits(AccessRequirement.permission("users.manage")));
        assertFalse(service.permits(AccessRequirement.permission("plugins.publish")));
        assertEquals(Arrays.asList("credential-change-required", "success"), audit.outcomes());
    }

    @Test public void rejectsInvalidLoginWithoutDisclosingAccountAndClearsPassword() {
        MemoryStore store = new MemoryStore(false);
        Events audit = new Events();
        DefaultIdentityService service = new DefaultIdentityService(store, new PlainHasher(), audit);
        char[] password = "wrong".toCharArray();
        assertEquals(AuthenticationResult.Status.INVALID, service.authenticate("admin", password).status());
        assertFalse(service.currentIdentity().isPresent());
        assertCleared(password);
        assertEquals(Collections.singletonList("denied"), audit.outcomes());
    }

    @Test public void rejectsReusingInitialPassword() {
        MemoryStore store = new MemoryStore(true);
        DefaultIdentityService service = new DefaultIdentityService(store, new PlainHasher(), new Events());
        assertEquals(AuthenticationResult.Status.INVALID,
                service.changePassword("admin", "companyware".toCharArray(), "companyware".toCharArray()).status());
        assertFalse(service.currentIdentity().isPresent());
    }

    @Test public void rejectsKnownInitialPasswordEvenAfterItWasChanged() {
        MemoryStore store = new MemoryStore(false);
        store.password = "a-different-password";
        DefaultIdentityService service = new DefaultIdentityService(store, new PlainHasher(), new Events());
        assertEquals(AuthenticationResult.Status.INVALID,
                service.changePassword("admin", "a-different-password".toCharArray(), "companyware".toCharArray()).status());
    }

    @Test public void logoutDropsTheAuthenticatedIdentity() {
        MemoryStore store = new MemoryStore(false);
        DefaultIdentityService service = new DefaultIdentityService(store, new PlainHasher(), new Events());
        assertTrue(service.authenticate("admin", "companyware".toCharArray()).isAuthenticated());
        service.logout();
        assertFalse(service.currentIdentity().isPresent());
    }

    private static void assertCleared(char[] value) { for (char item : value) assertEquals('\0', item); }

    private static final class MemoryStore implements UserAccountStore {
        private String password = "companyware";
        private boolean change;
        MemoryStore(boolean change) { this.change = change; }
        public Optional<UserAccount> findActive(String username) {
            if (!"admin".equals(username)) return Optional.empty();
            return Optional.<UserAccount>of(new UserAccount() {
                public String username() { return "admin"; }
                public String passwordHash() { return password; }
                public boolean active() { return true; }
                public boolean passwordChangeRequired() { return change; }
                public java.util.Set<String> roles() { return Collections.singleton("ADMIN"); }
                public java.util.Set<String> permissions() { return new LinkedHashSet<String>(Arrays.asList("users.manage")); }
            });
        }
        public void replacePassword(String username, String passwordHash) { password = passwordHash; change = false; }
    }
    private static final class PlainHasher implements PasswordHasher {
        public boolean matches(char[] clearText, String hash) { return clearText != null && new String(clearText).equals(hash); }
        public String hash(char[] clearText) { return new String(clearText); }
    }
    private static final class Events implements platform.api.security.AuditSink {
        private final List<AuditEvent> values = new ArrayList<AuditEvent>();
        public void record(AuditEvent event) { values.add(event); }
        List<String> outcomes() {
            List<String> result = new ArrayList<String>();
            for (AuditEvent value : values) result.add(value.outcome());
            return result;
        }
    }
}
