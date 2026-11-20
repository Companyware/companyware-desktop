package platform.api.security;

import platform.api.service.Service;

public interface AuthenticationService extends Service {
    AuthenticationResult authenticate(String username, char[] password);
    AuthenticationResult changePassword(String username, char[] currentPassword, char[] newPassword);
    void logout();
}
