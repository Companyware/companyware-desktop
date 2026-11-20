package platform.api.configuration;

import java.util.Optional;

/** Read-only, non-secret plugin-scoped configuration. Use SecretProvider for secrets. */
public interface PluginConfiguration {
    Optional<String> get(String key);
    boolean contains(String key);
}
