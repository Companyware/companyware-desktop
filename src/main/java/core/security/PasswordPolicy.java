package core.security;

/** Central password rules used by login, profile and administrative resets. */
public final class PasswordPolicy {
    public static final int MINIMUM_LENGTH = 10;
    private static final String INITIAL_PASSWORD = "companyware";

    private PasswordPolicy() { }

    public static boolean accepts(char[] password) {
        return password != null && password.length >= MINIMUM_LENGTH
                && !INITIAL_PASSWORD.equals(new String(password));
    }
}
