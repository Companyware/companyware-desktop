package platform.api.security;

import java.util.Objects;
import platform.api.plugin.PluginId;

/** Namespace-safe privilege owned by the declaring plugin. */
public final class PermissionId {
    private final PluginId owner;
    private final String name;
    private PermissionId(PluginId owner, String name) {
        this.owner = Objects.requireNonNull(owner, "owner");
        if (name == null || !name.matches("[a-z][a-z0-9.-]*")) throw new IllegalArgumentException("Invalid permission name");
        this.name = name;
    }
    public static PermissionId of(PluginId owner, String name) { return new PermissionId(owner, name); }
    public PluginId owner() { return owner; }
    public String name() { return name; }
    public String value() { return owner.value() + ":" + name; }
    @Override public String toString() { return value(); }
    @Override public boolean equals(Object other) { return other instanceof PermissionId && value().equals(((PermissionId) other).value()); }
    @Override public int hashCode() { return value().hashCode(); }
}
