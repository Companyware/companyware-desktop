package platform.api.theme;

import platform.api.plugin.PluginId;
import platform.api.version.SemanticVersion;
import platform.api.service.Service;

/** Versioned provider for a complete theme or semantic overlay. */
public interface ThemeProvider extends Service {
    PluginId id();
    String displayName();
    SemanticVersion version();
    boolean overlay();
    default int priority() { return 0; }
    ThemeTokens tokens();
}
