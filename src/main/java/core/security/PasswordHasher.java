package core.security;

public interface PasswordHasher {
    boolean matches(char[] clearText, String hash);
    String hash(char[] clearText);
}
