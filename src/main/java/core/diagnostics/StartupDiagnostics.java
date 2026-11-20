package core.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;
import persistence.PersistenceSettings;

/** Builds a non-secret startup inventory suitable for support diagnostics. */
public final class StartupDiagnostics {
    private StartupDiagnostics() { }

    public static Map<String, Object> capture(PersistenceSettings persistence, int pluginCount, int failedPlugins) {
        Map<String, Object> fields = new LinkedHashMap<String, Object>();
        Package application = StartupDiagnostics.class.getPackage();
        String version = application == null ? null : application.getImplementationVersion();
        fields.put("applicationVersion", version == null ? "development" : version);
        fields.put("javaVersion", System.getProperty("java.version", "unknown"));
        fields.put("os", System.getProperty("os.name", "unknown"));
        fields.put("persistenceProfile", persistence.getProfile().configurationValue());
        fields.put("plugins", pluginCount);
        fields.put("failedPlugins", failedPlugins);
        return fields;
    }
}
