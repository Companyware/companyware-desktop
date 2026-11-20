package platform.api.service;

import java.util.Objects;

/** Stable identity for a capability declared or required by a plugin. */
public final class CapabilityId {
    private final String value;

    private CapabilityId(String value) { this.value = value; }

    public static CapabilityId of(String value) {
        Objects.requireNonNull(value, "capability id");
        if (!value.matches("^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid capability id: " + value);
        }
        return new CapabilityId(value);
    }

    public String value() { return value; }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(Object other) {
        return other instanceof CapabilityId && value.equals(((CapabilityId) other).value);
    }
}
