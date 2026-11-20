package platform.api.ui;

import platform.api.security.PermissionId;

/** Optional authentication and permission requirement for a visible contribution. */
public final class AccessRequirement {
    public static final AccessRequirement PUBLIC = new AccessRequirement(false, null);
    public static final AccessRequirement AUTHENTICATED = new AccessRequirement(true, null);
    private final boolean authenticated;
    private final String permission;

    private AccessRequirement(boolean authenticated, String permission) {
        this.authenticated = authenticated;
        this.permission = permission;
    }
    public static AccessRequirement permission(String permission) {
        if (permission == null || permission.trim().isEmpty()) throw new IllegalArgumentException("permission is required");
        return new AccessRequirement(true, permission.trim());
    }
    public static AccessRequirement permission(PermissionId permission) {
        if (permission == null) throw new IllegalArgumentException("permission is required");
        return permission(permission.value());
    }
    public boolean requiresAuthentication() { return authenticated; }
    public String permission() { return permission; }
}
