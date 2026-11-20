package core.security;

import java.util.Optional;

public interface UserAccountStore {
    Optional<UserAccount> findActive(String username);
    void replacePassword(String username, String passwordHash);
}
