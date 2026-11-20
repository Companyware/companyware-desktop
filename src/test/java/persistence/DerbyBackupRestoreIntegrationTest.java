/*
 * Companyware
 * Copyright (c) 2026 Companyware contributors
 *
 * SPDX-License-Identifier: MIT
 * Licensed under the MIT License; see LICENSE in the project root.
 * This file is part of Companyware.
 */
package persistence;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.junit.Test;

public class DerbyBackupRestoreIntegrationTest {

    @Test
    public void restoresAnOnlineBackupIntoAnIndependentDatabase() throws Exception {
        Path root = Files.createTempDirectory("companyware-derby-backup-");
        String databaseName = "source-" + UUID.randomUUID().toString();
        Path source = root.resolve(databaseName);
        Path backup = root.resolve("backup");
        Path restored = root.resolve("restored");

        try {
            try (Connection connection = DriverManager.getConnection("jdbc:derby:" + source + ";create=true")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE CW_BACKUP_PROBE (ID INT PRIMARY KEY, VALUE VARCHAR(64))");
                    statement.executeUpdate("INSERT INTO CW_BACKUP_PROBE VALUES (1, 'verified')");
                }
                try (CallableStatement statement = connection.prepareCall("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)")) {
                    statement.setString(1, backup.toString());
                    statement.execute();
                }
            }

            Path backupDatabase = backup.resolve(databaseName);
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:derby:" + restored + ";restoreFrom=" + backupDatabase)) {
                try (Statement statement = connection.createStatement();
                        ResultSet result = statement.executeQuery("SELECT VALUE FROM CW_BACKUP_PROBE WHERE ID = 1")) {
                    result.next();
                    assertEquals("verified", result.getString(1));
                }
            }
            shutdown(source);
            shutdown(restored);
        } finally {
            deleteRecursively(root);
        }
    }

    private static void shutdown(Path database) throws Exception {
        try {
            DriverManager.getConnection("jdbc:derby:" + database + ";shutdown=true");
        } catch (SQLException expected) {
            assertEquals("08006", expected.getSQLState());
        }
    }

    private static void deleteRecursively(Path path) throws Exception {
        if (!Files.exists(path)) {
            return;
        }
        Files.walk(path)
                .sorted((left, right) -> right.compareTo(left))
                .forEach(item -> {
                    try {
                        Files.deleteIfExists(item);
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                });
    }
}
