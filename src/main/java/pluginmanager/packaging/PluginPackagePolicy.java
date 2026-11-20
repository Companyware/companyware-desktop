package pluginmanager.packaging;

/** Resource limits applied before a plugin package can be installed. */
public final class PluginPackagePolicy {
    public static final PluginPackagePolicy DEFAULT = new PluginPackagePolicy(1024, 50L * 1024 * 1024,
            100L * 1024 * 1024, 100);

    private final int maximumEntries;
    private final long maximumEntryBytes;
    private final long maximumTotalBytes;
    private final int maximumCompressionRatio;

    public PluginPackagePolicy(int maximumEntries, long maximumEntryBytes,
            long maximumTotalBytes, int maximumCompressionRatio) {
        if (maximumEntries < 1 || maximumEntryBytes < 1 || maximumTotalBytes < 1
                || maximumCompressionRatio < 1) throw new IllegalArgumentException("All package limits must be positive");
        this.maximumEntries = maximumEntries;
        this.maximumEntryBytes = maximumEntryBytes;
        this.maximumTotalBytes = maximumTotalBytes;
        this.maximumCompressionRatio = maximumCompressionRatio;
    }

    int maximumEntries() { return maximumEntries; }
    long maximumEntryBytes() { return maximumEntryBytes; }
    long maximumTotalBytes() { return maximumTotalBytes; }
    int maximumCompressionRatio() { return maximumCompressionRatio; }
}
