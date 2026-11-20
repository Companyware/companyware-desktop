package platform.api.plugin;

import java.util.Objects;

/** Safe diagnostic exposed without leaking a plugin implementation exception. */
public final class PluginDiagnostic {
    private final PluginId pluginId;
    private final PluginState state;
    private final String message;

    public PluginDiagnostic(PluginId pluginId, PluginState state, String message) {
        this.pluginId = Objects.requireNonNull(pluginId, "pluginId");
        this.state = Objects.requireNonNull(state, "state");
        this.message = Objects.requireNonNull(message, "message");
    }

    public PluginId getPluginId() { return pluginId; }
    public PluginState getState() { return state; }
    public String getMessage() { return message; }
}
