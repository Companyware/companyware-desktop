package platform.api.event;

import java.util.Objects;
import platform.api.plugin.PluginId;

/** Typed event identity with explicit plugin ownership. */
public final class EventKey<T> {
    private final PluginId owner;
    private final String name;
    private final Class<T> payloadType;

    private EventKey(PluginId owner, String name, Class<T> payloadType) {
        this.owner = Objects.requireNonNull(owner, "owner");
        if (name == null || !name.matches("^[a-z][a-z0-9-]*$")) {
            throw new IllegalArgumentException("Invalid event name: " + name);
        }
        this.name = name;
        this.payloadType = Objects.requireNonNull(payloadType, "payloadType");
    }

    public static <T> EventKey<T> of(PluginId owner, String name, Class<T> payloadType) {
        return new EventKey<T>(owner, name, payloadType);
    }

    public PluginId owner() { return owner; }
    public String name() { return name; }
    public Class<T> payloadType() { return payloadType; }
}
