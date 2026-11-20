package core.security;

import java.util.Set;

public interface UserAccount {
    String username();
    String passwordHash();
    boolean active();
    boolean passwordChangeRequired();
    Set<String> roles();
    Set<String> permissions();
}
