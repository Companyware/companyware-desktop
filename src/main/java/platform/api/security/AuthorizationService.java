package platform.api.security;

import java.util.Optional;
import platform.api.service.Service;
import platform.api.ui.AccessRequirement;

public interface AuthorizationService extends Service {
    Optional<UserIdentity> currentIdentity();
    boolean hasPermission(String permission);
    boolean permits(AccessRequirement requirement);
}
