package platform.api.persistence;

import platform.api.service.ServiceKey;

public final class PersistenceServiceIds {
    public static final ServiceKey<PluginDatabase> PLUGIN_DATABASE =
            ServiceKey.of("platform.plugin-database", PluginDatabase.class);
    private PersistenceServiceIds() { }
}
