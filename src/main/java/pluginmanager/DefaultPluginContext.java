package pluginmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import platform.api.configuration.PluginConfiguration;
import platform.api.event.EventKey;
import platform.api.event.EventPublisher;
import platform.api.i18n.PluginMessages;
import platform.api.i18n.LocalizationServiceIds;
import platform.api.plugin.PluginContext;
import platform.api.plugin.PluginDescriptor;
import platform.api.plugin.PluginId;
import platform.api.service.CapabilityId;
import platform.api.service.CapabilityRegistry;
import platform.api.service.Service;
import platform.api.service.ServiceKey;
import platform.api.theme.ThemeRegistry;
import platform.api.ui.PlatformServiceIds;
import platform.api.ui.UiRegistry;

/** Small SDK context with safe defaults; advanced facilities remain optional for plugin authors. */
final class DefaultPluginContext implements PluginContext {
    private final PluginDescriptor descriptor;
    private final PluginManagerImpl manager;
    private final List<AutoCloseable> resources = new ArrayList<AutoCloseable>();
    private final Map<CapabilityId, Set<PluginId>> capabilities = new LinkedHashMap<CapabilityId, Set<PluginId>>();

    DefaultPluginContext(PluginDescriptor descriptor, PluginManagerImpl manager) {
        this.descriptor = descriptor;
        this.manager = manager;
        for (CapabilityId capability : descriptor.getCapabilities()) registerCapability(descriptor.getId(), capability);
    }

    @Override public PluginDescriptor descriptor() { return descriptor; }
    @Override public PluginConfiguration configuration() {
        return new PluginConfiguration() {
            public Optional<String> get(String key) { return Optional.empty(); }
            public boolean contains(String key) { return false; }
        };
    }
    @Override public PluginMessages messages() {
		String entryPoint = descriptor.getEntryPoint();
		String baseName = entryPoint.substring(0, entryPoint.lastIndexOf('.')) + ".messages";
		return manager.getService(LocalizationServiceIds.LOCALIZATION).get().messages(baseName);
    }
    @Override public EventPublisher events() {
        return new EventPublisher() {
            public <T> void publish(EventKey<T> event, T payload) {
                if (payload != null) event.payloadType().cast(payload);
            }
        };
    }
    @Override public CapabilityRegistry capabilities() {
        return new CapabilityRegistry() {
            public void register(PluginId provider, CapabilityId capability) { registerCapability(provider, capability); }
            public boolean isAvailable(CapabilityId capability) { return capabilities.containsKey(capability); }
            public Set<PluginId> providers(CapabilityId capability) {
                Set<PluginId> providers = capabilities.get(capability);
                return providers == null ? Collections.<PluginId>emptySet() : Collections.unmodifiableSet(providers);
            }
        };
    }
    @Override public <T extends Service> void register(final ServiceKey<T> key, final T service) {
        manager.registerService(key, service);
        closeOnStop(() -> manager.unregisterService(key, service));
    }
    @Override public <T extends Service> Optional<T> find(ServiceKey<T> key) { return manager.getService(key); }
    @Override public UiRegistry ui() { return manager.getService(PlatformServiceIds.UI_REGISTRY).get(); }
    @Override public ThemeRegistry themes() { return manager.getService(PlatformServiceIds.THEME_REGISTRY).get(); }
    @Override public void closeOnStop(AutoCloseable resource) { resources.add(java.util.Objects.requireNonNull(resource, "resource")); }

    void closeResources() throws Exception {
        Exception first = null;
        for (int index = resources.size() - 1; index >= 0; index--) {
            try { resources.get(index).close(); }
            catch (Exception exception) { if (first == null) first = exception; }
        }
        resources.clear();
        if (first != null) throw first;
    }

    private void registerCapability(PluginId provider, CapabilityId capability) {
        Set<PluginId> providers = capabilities.get(capability);
        if (providers == null) capabilities.put(capability, providers = new LinkedHashSet<PluginId>());
        providers.add(provider);
    }
}
