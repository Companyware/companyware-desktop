package platform.api.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import platform.api.ApiVersion;
import platform.api.service.CapabilityId;
import platform.api.version.SemanticVersion;

/** Validated metadata required before plugin code may be loaded. */
public final class PluginDescriptor {
    private final PluginId id;
    private final String name;
    private final String author;
    private final SemanticVersion version;
    private final SemanticVersion requiredApiVersion;
    private final String entryPoint;
    private final int priority;
    private final List<PluginDependency> dependencies;
    private final Set<CapabilityId> capabilities;

    public PluginDescriptor(PluginId id, String name, String author, SemanticVersion version,
            SemanticVersion requiredApiVersion, String entryPoint,
            int priority, List<PluginDependency> dependencies, Set<CapabilityId> capabilities) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = requireText(name, "name");
        this.author = requireText(author, "author");
        this.version = Objects.requireNonNull(version, "version");
        this.requiredApiVersion = Objects.requireNonNull(requiredApiVersion, "requiredApiVersion");
        this.entryPoint = requireEntryPoint(entryPoint);
        this.priority = priority;
        if (!ApiVersion.CURRENT.isApiCompatibleWith(requiredApiVersion)) {
            throw new IllegalArgumentException("Plugin " + id + " requires incompatible API " + requiredApiVersion);
        }
        this.dependencies = Collections.unmodifiableList(new ArrayList<PluginDependency>(
                dependencies == null ? Collections.<PluginDependency>emptyList() : dependencies));
        this.capabilities = Collections.unmodifiableSet(new LinkedHashSet<CapabilityId>(
                capabilities == null ? Collections.<CapabilityId>emptySet() : capabilities));
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String requireEntryPoint(String value) {
        String entryPoint = requireText(value, "entryPoint");
        if (!entryPoint.matches("[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+")) {
            throw new IllegalArgumentException("Invalid entryPoint: " + entryPoint);
        }
        return entryPoint;
    }

    public PluginId getId() { return id; }
    public String getName() { return name; }
    public String getAuthor() { return author; }
    public SemanticVersion getVersion() { return version; }
    public SemanticVersion getRequiredApiVersion() { return requiredApiVersion; }
    public String getEntryPoint() { return entryPoint; }
    public int getPriority() { return priority; }
    public List<PluginDependency> getDependencies() { return dependencies; }
    public Set<CapabilityId> getCapabilities() { return capabilities; }
}
