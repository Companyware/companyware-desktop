package platform.api.application;

/** Stable, transport-neutral error categories for application use cases. */
public enum ApplicationErrorCode {
    VALIDATION,
    NOT_FOUND,
    CONFLICT,
    UNAVAILABLE,
    INTERNAL
}
