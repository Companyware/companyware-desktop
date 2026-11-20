package platform.api.application;

import java.util.Objects;
import java.util.Optional;

/** Explicit success/failure result usable by Swing, local and future remote adapters. */
public final class ApplicationResult<T> {
    private final T value;
    private final ApplicationErrorCode errorCode;
    private final String message;

    private ApplicationResult(T value, ApplicationErrorCode errorCode, String message) {
        this.value = value;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static <T> ApplicationResult<T> success(T value) {
        return new ApplicationResult<T>(value, null, "");
    }

    public static <T> ApplicationResult<T> failure(ApplicationErrorCode code, String message) {
        return new ApplicationResult<T>(null, Objects.requireNonNull(code, "code"),
                message == null ? "" : message);
    }

    public boolean isSuccess() { return errorCode == null; }
    public Optional<T> value() { return Optional.ofNullable(value); }
    public Optional<ApplicationErrorCode> errorCode() { return Optional.ofNullable(errorCode); }
    public String message() { return message; }

    public T valueOrThrow() {
        if (!isSuccess()) throw new IllegalStateException(errorCode + ": " + message);
        return value;
    }
}
