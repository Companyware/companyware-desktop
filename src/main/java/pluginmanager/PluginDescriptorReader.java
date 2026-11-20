package pluginmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import platform.api.plugin.PluginDependency;
import platform.api.plugin.PluginDescriptor;
import platform.api.plugin.PluginId;
import platform.api.service.CapabilityId;
import platform.api.version.SemanticVersion;

/** Reads and validates descriptor data without loading plugin classes. */
public final class PluginDescriptorReader {
    private static final Set<String> ALLOWED_FIELDS = new LinkedHashSet<String>(Arrays.asList(
            "id", "name", "author", "version", "requiredApiVersion", "entryPoint",
            "priority", "capabilities", "dependencies"));
    private static final Set<String> ALLOWED_DEPENDENCY_FIELDS = new LinkedHashSet<String>(Arrays.asList(
            "id", "minimumVersion"));

    private PluginDescriptorReader() { }

    @SuppressWarnings("unchecked")
    public static PluginDescriptor read(File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("Plugin descriptor does not exist: " + file);
        }
        try (InputStream input = new FileInputStream(file)) {
            return read(input, file.toString());
        }
    }

    @SuppressWarnings("unchecked")
    public static PluginDescriptor read(InputStream input, String source) throws IOException {
        if (input == null) throw new IllegalArgumentException("Plugin descriptor input is required");
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(20);
        options.setCodePointLimit(1_000_000);
        Object document = new Yaml(new SafeConstructor(options)).load(input);
        if (!(document instanceof Map)) {
            throw new IllegalArgumentException("Plugin descriptor must be a mapping: " + source);
        }
        Map<String, Object> values = (Map<String, Object>) document;

        for (String field : values.keySet()) {
            if (!ALLOWED_FIELDS.contains(field)) {
                throw new IllegalArgumentException("Unknown descriptor field: " + field);
            }
        }

        List<PluginDependency> dependencies = new ArrayList<PluginDependency>();
        Object dependencyValue = values.get("dependencies");
        if (dependencyValue != null && !(dependencyValue instanceof List)) {
            throw new IllegalArgumentException("dependencies must be a list");
        }
        if (dependencyValue instanceof List) {
            for (Object item : (List<?>) dependencyValue) {
                if (!(item instanceof Map)) throw new IllegalArgumentException("Invalid dependency in " + source);
                Map<String, Object> dependency = (Map<String, Object>) item;
                for (String field : dependency.keySet()) {
                    if (!ALLOWED_DEPENDENCY_FIELDS.contains(field)) {
                        throw new IllegalArgumentException("Unknown dependency field: " + field);
                    }
                }
                dependencies.add(new PluginDependency(
                        PluginId.of(text(dependency, "id")),
                        SemanticVersion.parse(text(dependency, "minimumVersion"))));
            }
        }

        Set<CapabilityId> capabilities = new LinkedHashSet<CapabilityId>();
        Object capabilityValue = values.get("capabilities");
        if (!(capabilityValue instanceof List)) throw new IllegalArgumentException("capabilities must be a list");
        for (Object item : (List<?>) capabilityValue) capabilities.add(CapabilityId.of(String.valueOf(item)));

        return new PluginDescriptor(
                PluginId.of(text(values, "id")),
                text(values, "name"),
                text(values, "author"),
                SemanticVersion.parse(text(values, "version")),
                SemanticVersion.parse(text(values, "requiredApiVersion")),
                text(values, "entryPoint"),
                integer(values, "priority", 0),
                dependencies,
                capabilities);
    }

    private static String text(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            throw new IllegalArgumentException("Missing descriptor field: " + key);
        }
        return String.valueOf(value).trim();
    }

    private static int integer(Map<String, Object> values, String key, int defaultValue) {
        Object value = values.get(key);
        if (value == null) return defaultValue;
        if (!(value instanceof Number)) throw new IllegalArgumentException(key + " must be an integer");
        return ((Number) value).intValue();
    }
}
