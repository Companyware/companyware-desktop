package platform.api.persistence;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable, versioned SQL migration owned by the calling plugin. */
public final class PluginMigration {
    private final int version;
    private final String description;
    private final String sql;
    private final List<String> acceptedPreviousChecksums;

    public PluginMigration(int version, String description, String sql) {
        this(version, description, sql, new String[0]);
    }

    public PluginMigration(int version, String description, String sql, String... acceptedPreviousChecksums) {
        if (version < 1) throw new IllegalArgumentException("version must be positive");
        this.version = version;
        this.description = Objects.requireNonNull(description, "description");
        this.sql = Objects.requireNonNull(sql, "sql");
        this.acceptedPreviousChecksums = Collections.unmodifiableList(
                Arrays.asList(acceptedPreviousChecksums.clone()));
    }
    public int version() { return version; }
    public String description() { return description; }
    public String sql() { return sql; }
    public List<String> acceptedPreviousChecksums() { return acceptedPreviousChecksums; }
}
