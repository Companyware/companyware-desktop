package platform.api.plugin;

import java.util.Objects;

/** Current lifecycle health of one discovered plugin, safe for diagnostics UIs and logs. */
public final class PluginHealth {
    private final PluginId pluginId;
    private final PluginState state;
    private final String message;

    public PluginHealth(PluginId pluginId, PluginState state, String message) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.state = Objects.requireNonNull(state, "state");
        this.message = message == null ? "" : message;
    }

    public PluginId getPluginId() { return pluginId; }
    public PluginState getState() { return state; }
    public String getMessage() { return message; }
    public boolean isHealthy() { return state != PluginState.FAILED; }
}
