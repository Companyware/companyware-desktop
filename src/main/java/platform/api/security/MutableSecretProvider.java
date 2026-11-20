package platform.api.security;

/** Allows trusted settings views to replace a secret for the current process only. */
public interface MutableSecretProvider extends SecretProvider {
    void setForSession(String name, char[] value);
    void clearSessionValue(String name);
    /** Removes and zeroes every secret held only for the authenticated process session. */
    void clearSessionValues();
}
