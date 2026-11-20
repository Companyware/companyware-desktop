package persistence.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Coordinates deterministic, transactional migrations identically for every Derby profile. */
public final class MigrationRunner {
    /** Records a verified pre-migration schema as already applied during legacy adoption. */
    public void baseline(Connection connection, Migration migration) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            ensureHistoryTable(connection);
            String applied = appliedChecksum(connection, migration);
            if (applied == null) record(connection, migration);
            else if (!migration.acceptsChecksum(applied)) {
                throw new IllegalStateException("Applied migration was modified: " + migration.key());
            }
            connection.commit();
        } catch (RuntimeException | SQLException failure) {
            connection.rollback();
            throw failure;
        } finally { connection.setAutoCommit(originalAutoCommit); }
    }

    public void migrate(Connection connection, List<Migration> migrations) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            ensureHistoryTable(connection);
            connection.commit();
            List<Migration> ordered = new ArrayList<Migration>(migrations);
            Collections.sort(ordered);
            Set<String> seen = new HashSet<String>();
            for (Migration migration : ordered) {
                if (!seen.add(migration.key())) throw new IllegalArgumentException("Duplicate migration: " + migration.key());
                String appliedChecksum = appliedChecksum(connection, migration);
                if (appliedChecksum != null) {
                    if (!migration.acceptsChecksum(appliedChecksum)) {
                        throw new IllegalStateException("Applied migration was modified: " + migration.key());
                    }
                    continue;
                }
                try {
                    executeStatements(connection, migration.getSql());
                    record(connection, migration);
                    connection.commit();
                } catch (Exception failure) {
                    connection.rollback();
                    if (failure instanceof SQLException) throw (SQLException) failure;
                    if (failure instanceof RuntimeException) throw (RuntimeException) failure;
                    throw new SQLException("Migration failed: " + migration.key(), failure);
                }
            }
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private void ensureHistoryTable(Connection connection) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, "CW_SCHEMA_MIGRATION", new String[] {"TABLE"})) {
            if (tables.next()) return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE CW_SCHEMA_MIGRATION (OWNER_ID VARCHAR(128) NOT NULL, VERSION BIGINT NOT NULL, DESCRIPTION VARCHAR(255) NOT NULL, CHECKSUM CHAR(64) NOT NULL, APPLIED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, PRIMARY KEY (OWNER_ID, VERSION))");
        }
    }

    private String appliedChecksum(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT CHECKSUM FROM CW_SCHEMA_MIGRATION WHERE OWNER_ID=? AND VERSION=?")) {
            query.setString(1, migration.getOwner().value());
            query.setLong(2, migration.getVersion());
            try (ResultSet result = query.executeQuery()) { return result.next() ? result.getString(1) : null; }
        }
    }

    private void executeStatements(Connection connection, String sql) throws SQLException {
        for (String fragment : sql.split(";")) {
            String statementText = fragment.trim();
            if (statementText.isEmpty()) continue;
            try (Statement statement = connection.createStatement()) { statement.executeUpdate(statementText); }
        }
    }

    private void record(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO CW_SCHEMA_MIGRATION (OWNER_ID, VERSION, DESCRIPTION, CHECKSUM) VALUES (?, ?, ?, ?)")) {
            insert.setString(1, migration.getOwner().value());
            insert.setLong(2, migration.getVersion());
            insert.setString(3, migration.getDescription());
            insert.setString(4, migration.checksum());
            insert.executeUpdate();
        }
    }
}
