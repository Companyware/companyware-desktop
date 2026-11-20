package core.security;

import java.util.Optional;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import platform.api.security.MutableSecretProvider;

/** Resolves CW_SECRET_NAME first from the environment, then a JVM property. */
public final class EnvironmentSecretProvider implements MutableSecretProvider {
    private final Map<String, char[]> sessionValues = new ConcurrentHashMap<String, char[]>();
    @Override public Optional<char[]> find(String name) {
        validate(name);
        char[] session = sessionValues.get(name);
        if (session != null) return Optional.of(Arrays.copyOf(session, session.length));
        String key = "CW_SECRET_" + name.toUpperCase().replace('.', '_').replace('-', '_');
        String value = System.getenv(key);
        if (value == null) value = System.getProperty("companyware.secret." + name);
        return value == null ? Optional.<char[]>empty() : Optional.of(value.toCharArray());
    }
    @Override public synchronized void setForSession(String name, char[] value) {
        validate(name);
        if (value == null || value.length == 0) throw new IllegalArgumentException("Secret value is required");
        char[] replacement = Arrays.copyOf(value, value.length);
        char[] previous = sessionValues.put(name, replacement);
        if (previous != null) Arrays.fill(previous, '\0');
    }
    @Override public synchronized void clearSessionValue(String name) {
        validate(name);
        char[] previous = sessionValues.remove(name);
        if (previous != null) Arrays.fill(previous, '\0');
    }
    @Override public synchronized void clearSessionValues() {
        for (char[] previous : sessionValues.values()) Arrays.fill(previous, '\0');
        sessionValues.clear();
    }
    private static void validate(String name) {
        if (name == null || !name.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("Invalid secret name");
    }
}
