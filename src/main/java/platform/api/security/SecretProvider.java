package platform.api.security;

import java.util.Optional;
import platform.api.service.Service;

/** Retrieves deployment secrets kept outside source code and plugin descriptors. */
public interface SecretProvider extends Service {
    Optional<char[]> find(String name);
}
