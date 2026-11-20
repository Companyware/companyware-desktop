package core.security;

import java.util.Optional;
import models.user.UserModel;

public final class HibernateUserAccountStore implements UserAccountStore {
    @Override public Optional<UserAccount> findActive(String username) {
        final UserModel user = models.user.Repository.getActiveUserByUsername(username);
        if (user == null) return Optional.empty();
        return Optional.<UserAccount>of(new UserAccount() {
            public String username() { return user.getUsername(); }
            public String passwordHash() { return user.getPassword(); }
            public boolean active() { return Boolean.TRUE.equals(user.getActive()); }
            public boolean passwordChangeRequired() {
                return Boolean.TRUE.equals(user.getPasswordChangeRequired())
                        || (user.getPasswordChangeRequired() == null && "companyware".equals(user.getUsername()));
            }
            public java.util.Set<String> roles() { return user.roleSet(); }
            public java.util.Set<String> permissions() { return user.permissionSet(); }
        });
    }
    @Override public void replacePassword(String username, String passwordHash) {
        UserModel user = models.user.Repository.getActiveUserByUsername(username);
        if (user == null) throw new IllegalArgumentException("Unknown active user");
        user.setPassword(passwordHash);
        user.setPasswordChangeRequired(false);
        models.user.Repository.save(user);
    }
}
