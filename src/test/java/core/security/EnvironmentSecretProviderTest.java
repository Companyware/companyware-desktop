package core.security;

import static org.junit.Assert.*;
import org.junit.Test;

public class EnvironmentSecretProviderTest {
    @Test public void sessionValueOverridesAndCanBeCleared() {
        EnvironmentSecretProvider provider = new EnvironmentSecretProvider();
        char[] supplied = "temporary".toCharArray();
        provider.setForSession("invoice.smtp.password", supplied);
        java.util.Arrays.fill(supplied, 'x');
        assertEquals("temporary", new String(provider.find("invoice.smtp.password").get()));
        provider.clearSessionValue("invoice.smtp.password");
        assertTrue(!provider.find("invoice.smtp.password").isPresent());
    }
    @Test public void readsJvmSecretOutsidePluginConfiguration() {
        String key = "companyware.secret.test-key";
        System.setProperty(key, "private-value");
        try {
            char[] secret = new EnvironmentSecretProvider().find("test-key").get();
            assertArrayEquals("private-value".toCharArray(), secret);
        } finally { System.clearProperty(key); }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsafeSecretNames() { new EnvironmentSecretProvider().find("../secret"); }

    @Test public void logoutStyleClearRemovesEverySessionSecret() {
        EnvironmentSecretProvider provider = new EnvironmentSecretProvider();
        provider.setForSession("one", "first".toCharArray());
        provider.setForSession("two", "second".toCharArray());
        provider.clearSessionValues();
        assertFalse(provider.find("one").isPresent());
        assertFalse(provider.find("two").isPresent());
    }
}
