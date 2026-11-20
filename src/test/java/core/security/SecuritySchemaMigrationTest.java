package core.security;

import static org.junit.Assert.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.junit.Test;

public class SecuritySchemaMigrationTest {
    @Test public void upgradesAnExistingUserTableIdempotently() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        String database = "memory:security-" + UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection("jdbc:derby:" + database + ";create=true")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE Users (id BIGINT, username VARCHAR(100), password VARCHAR(100), active BOOLEAN)");
                statement.executeUpdate("INSERT INTO Users (id, username, password, active) VALUES (1, 'companyware', 'hash', TRUE)");
            }
            SecuritySchemaMigration.migrate(connection);
            SecuritySchemaMigration.migrate(connection);
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT password_change_required, roles, permissions, email_verified, email_verified_at FROM Users WHERE username='companyware'");
                    ResultSet result = query.executeQuery()) {
                assertTrue(result.next());
                assertTrue(result.getBoolean(1));
                assertEquals("ADMIN", result.getString(2));
                assertEquals("*", result.getString(3));
                assertFalse(result.getBoolean(4));
                assertNull(result.getTimestamp(5));
            }
            try (ResultSet table = connection.getMetaData().getTables(null, "APP", "ACCOUNTRECOVERYKEYS", new String[] {"TABLE"})) {
                assertTrue(table.next());
            }
            try (ResultSet table = connection.getMetaData().getTables(null, "APP", "ACCOUNTEMAILTOKENS", new String[] {"TABLE"})) {
                assertTrue(table.next());
            }
        } finally {
            try { DriverManager.getConnection("jdbc:derby:" + database + ";drop=true"); }
            catch (java.sql.SQLException expected) { assertEquals("08006", expected.getSQLState()); }
        }
    }
}
