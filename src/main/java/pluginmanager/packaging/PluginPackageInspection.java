package pluginmanager.packaging;

import java.util.Collections;
import java.util.List;
import platform.api.plugin.PluginDescriptor;

/** Immutable result of inspecting a package without executing plugin code. */
public final class PluginPackageInspection {
    private final PluginDescriptor descriptor;
    private final String sha256;
    private final List<String> entries;
    private final long uncompressedBytes;

    PluginPackageInspection(PluginDescriptor descriptor, String sha256, List<String> entries,
            long uncompressedBytes) {
        this.descriptor = descriptor;
        this.sha256 = sha256;
        this.entries = Collections.unmodifiableList(entries);
        this.uncompressedBytes = uncompressedBytes;
    }

    public PluginDescriptor getDescriptor() { return descriptor; }
    public String getSha256() { return sha256; }
    public List<String> getEntries() { return entries; }
    public long getUncompressedBytes() { return uncompressedBytes; }
}
