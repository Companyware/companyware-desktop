package platform.api.plugin;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable, lowercase identifier independent of a plugin's display name. */
public final class PluginId {
    private static final Pattern FORMAT = Pattern.compile("^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*$");
    private final String value;

    private PluginId(String value) { this.value = value; }

    public static PluginId of(String value) {
        Objects.requireNonNull(value, "plugin id");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid plugin id: " + value);
        }
        return new PluginId(value);
    }

    public String value() { return value; }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(Object other) {
        return other instanceof PluginId && value.equals(((PluginId) other).value);
    }
}
