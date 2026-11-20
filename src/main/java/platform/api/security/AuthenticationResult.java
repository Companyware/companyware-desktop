package platform.api.security;

import java.util.Optional;

/** Result deliberately avoids revealing whether a username exists. */
public final class AuthenticationResult {
    public enum Status { AUTHENTICATED, CREDENTIAL_CHANGE_REQUIRED, INVALID }
    private static final AuthenticationResult INVALID = new AuthenticationResult(Status.INVALID, null);
    private final Status status;
    private final UserIdentity identity;

    private AuthenticationResult(Status status, UserIdentity identity) {
        this.status = status;
        this.identity = identity;
    }
    public static AuthenticationResult authenticated(UserIdentity identity) {
        return new AuthenticationResult(Status.AUTHENTICATED, identity);
    }
    public static AuthenticationResult credentialChangeRequired(UserIdentity identity) {
        return new AuthenticationResult(Status.CREDENTIAL_CHANGE_REQUIRED, identity);
    }
    public static AuthenticationResult invalid() { return INVALID; }
    public Status status() { return status; }
    public Optional<UserIdentity> identity() { return Optional.ofNullable(identity); }
    public boolean isAuthenticated() { return status == Status.AUTHENTICATED; }
}
