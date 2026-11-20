package platform.api.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Authenticated identity exposed to plugins without persistence details. */
public final class UserIdentity {
    private final String username;
    private final Set<String> roles;
    private final Set<String> permissions;

    public UserIdentity(String username, Set<String> roles, Set<String> permissions) {
        this.username = require(username, "username");
        this.roles = immutable(roles);
        this.permissions = immutable(permissions);
    }

    public String username() { return username; }
    public Set<String> roles() { return roles; }
    public Set<String> permissions() { return permissions; }
    public boolean hasRole(String role) { return roles.contains(role); }
    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }

    private static Set<String> immutable(Set<String> values) {
        LinkedHashSet<String> copy = new LinkedHashSet<String>();
        if (values != null) for (String value : values) if (value != null && !value.trim().isEmpty()) copy.add(value.trim());
        return Collections.unmodifiableSet(copy);
    }

    private static String require(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
