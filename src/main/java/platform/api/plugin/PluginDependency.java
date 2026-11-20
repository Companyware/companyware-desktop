package platform.api.plugin;

import java.util.Objects;
import platform.api.version.SemanticVersion;

/** Minimum compatible version required from another plugin. */
public final class PluginDependency {
    private final PluginId pluginId;
    private final SemanticVersion minimumVersion;

    public PluginDependency(PluginId pluginId, SemanticVersion minimumVersion) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.minimumVersion = Objects.requireNonNull(minimumVersion, "minimumVersion");
    }

    public PluginId getPluginId() { return pluginId; }
    public SemanticVersion getMinimumVersion() { return minimumVersion; }
}
