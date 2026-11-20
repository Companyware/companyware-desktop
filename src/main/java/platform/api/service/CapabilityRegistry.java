package platform.api.service;

import java.util.Set;
import platform.api.plugin.PluginId;

/** Typed capability publication and discovery contract. */
public interface CapabilityRegistry extends Service {
    void register(PluginId provider, CapabilityId capability);
    boolean isAvailable(CapabilityId capability);
    Set<PluginId> providers(CapabilityId capability);
}
