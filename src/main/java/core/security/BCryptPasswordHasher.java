package core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class BCryptPasswordHasher implements PasswordHasher {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    @Override public boolean matches(char[] clearText, String hash) {
        return clearText != null && hash != null && encoder.matches(new String(clearText), hash);
    }
    @Override public String hash(char[] clearText) { return encoder.encode(new String(clearText)); }
}
