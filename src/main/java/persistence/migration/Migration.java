package persistence.migration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import platform.api.plugin.PluginId;

/** Versioned SQL owned by Core or one plugin. */
public final class Migration implements Comparable<Migration> {
    private final PluginId owner;
    private final long version;
    private final String description;
    private final String sql;
    private final Set<String> acceptedPreviousChecksums;

    public Migration(PluginId owner, long version, String description, String sql) {
        this(owner, version, description, sql, Collections.<String>emptySet());
    }

    public Migration(PluginId owner, long version, String description, String sql,
            Iterable<String> acceptedPreviousChecksums) {
        this.owner = Objects.requireNonNull(owner, "owner");
        if (version < 1) throw new IllegalArgumentException("Migration version must be positive");
        this.version = version;
        this.description = requireText(description, "description");
        this.sql = requireText(sql, "sql");
        Set<String> accepted = new HashSet<String>();
        for (String checksum : acceptedPreviousChecksums) accepted.add(requireText(checksum, "previous checksum"));
        this.acceptedPreviousChecksums = Collections.unmodifiableSet(accepted);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }

    public PluginId getOwner() { return owner; }
    public long getVersion() { return version; }
    public String getDescription() { return description; }
    public String getSql() { return sql; }
    public String key() { return owner.value() + ":" + version; }
    public String checksum() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sql.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte item : hash) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public boolean acceptsChecksum(String value) {
        return checksum().equals(value) || acceptedPreviousChecksums.contains(value);
    }
    @Override public int compareTo(Migration other) {
        int ownerOrder = owner.value().compareTo(other.owner.value());
        return ownerOrder != 0 ? ownerOrder : Long.compare(version, other.version);
    }
}
