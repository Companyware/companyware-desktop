package persistence.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;
import org.apache.derby.drda.NetworkServerControl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import platform.api.plugin.PluginId;
import core.security.SecuritySchemaMigration;

public class MigrationRunnerIntegrationTest {
    private Path temporary;
    private String embeddedUrl;

    @Before public void prepare() throws IOException { temporary = Files.createTempDirectory("companyware-persistence-"); }
    @After public void cleanup() throws IOException {
        System.clearProperty("derby.system.home");
        if (embeddedUrl != null) {
            try { DriverManager.getConnection(embeddedUrl + ";shutdown=true", "app", "ap"); }
            catch (Exception expectedShutdownSignal) { }
        }
        if (temporary == null || !Files.exists(temporary)) return;
        try (Stream<Path> paths = Files.walk(temporary)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }

    @Test public void samePluginMigrationRunsInEmbeddedProfile() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        embeddedUrl = "jdbc:derby:" + temporary.resolve("embedded-db").toString();
        try (Connection connection = DriverManager.getConnection(embeddedUrl + ";create=true", "app", "ap")) {
            verifyMigrationContract(connection);
        }
    }

    @Test public void samePluginMigrationRunsAgainstExternalNetworkServer() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        System.setProperty("derby.system.home", temporary.resolve("server-home").toString());
        Files.createDirectories(temporary.resolve("server-home"));
        NetworkServerControl server = new NetworkServerControl(InetAddress.getByName("localhost"), port);
        server.start(null);
        String networkUrl = null;
        try {
            waitUntilReady(server);
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            String networkDatabase = temporary.resolve("network-db").toAbsolutePath().toString().replace('\\', '/');
            networkUrl = "jdbc:derby://localhost:" + port + "/" + networkDatabase;
            try (Connection connection = DriverManager.getConnection(networkUrl + ";create=true", "app", "ap")) {
                verifyMigrationContract(connection);
            }
        } finally {
            if (networkUrl != null) try { DriverManager.getConnection(networkUrl + ";shutdown=true", "app", "ap"); }
            catch (java.sql.SQLException expectedShutdownSignal) { }
            server.shutdown();
        }
    }

    @Test public void adoptsVerifiedLegacySchemaWithoutRecreatingIt() throws Exception {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        embeddedUrl = "jdbc:derby:" + temporary.resolve("legacy-db").toString();
        try (Connection connection = DriverManager.getConnection(embeddedUrl + ";create=true", "app", "ap")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE LEGACY_ITEM (ID INT PRIMARY KEY)");
            }
            Migration legacy = new Migration(PluginId.of("legacy.plugin"), 1, "legacy table",
                    "CREATE TABLE LEGACY_ITEM (ID INT PRIMARY KEY)");
            MigrationRunner runner = new MigrationRunner();
            runner.baseline(connection, legacy);
            runner.migrate(connection, Arrays.asList(legacy));
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM CW_SCHEMA_MIGRATION WHERE OWNER_ID='legacy.plugin'")) {
                rows.next();
                assertEquals(1, rows.getInt(1));
            }
        }
    }

    private void verifyMigrationContract(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE Users (id BIGINT, username VARCHAR(100), password VARCHAR(100), active BOOLEAN)");
            statement.executeUpdate("INSERT INTO Users (id, username, password, active) VALUES (1, 'companyware', 'hash', TRUE)");
        }
        SecuritySchemaMigration.migrate(connection);
        try (ResultSet recovery = connection.getMetaData().getTables(null, "APP", "ACCOUNTRECOVERYKEYS", new String[] {"TABLE"})) {
            org.junit.Assert.assertTrue("Recovery migration must run in both Derby profiles", recovery.next());
        }
        try (ResultSet recovery = connection.getMetaData().getTables(null, "APP", "ACCOUNTEMAILTOKENS", new String[] {"TABLE"})) {
            org.junit.Assert.assertTrue("E-mail token migration must run in both Derby profiles", recovery.next());
        }
        Migration migration = new Migration(PluginId.of("sample.plugin"), 1, "sample table",
                "CREATE TABLE SAMPLE_ITEM (ID INT PRIMARY KEY, NAME VARCHAR(50));"
                + "INSERT INTO SAMPLE_ITEM (ID, NAME) VALUES (1, 'same')");
        MigrationRunner runner = new MigrationRunner();
        runner.migrate(connection, Arrays.asList(migration));
        runner.migrate(connection, Arrays.asList(migration));
        Migration relocated = new Migration(PluginId.of("sample.plugin"), 1, "sample table",
                "CREATE TABLE SAMPLE_ITEM (ID INT PRIMARY KEY, NAME VARCHAR(50));\n"
                + "INSERT INTO SAMPLE_ITEM (ID, NAME) VALUES (1, 'same')",
                java.util.Collections.singleton(migration.checksum()));
        runner.migrate(connection, Arrays.asList(relocated));
        try (Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM SAMPLE_ITEM")) {
            rows.next();
            assertEquals(1, rows.getInt(1));
        }
        try {
            runner.migrate(connection, Arrays.asList(new Migration(PluginId.of("sample.plugin"), 1,
                    "changed", "CREATE TABLE DIFFERENT_TABLE (ID INT)")));
            fail("Modified applied migration must be rejected");
        } catch (IllegalStateException expected) { }
    }

    private void waitUntilReady(NetworkServerControl server) throws Exception {
        Exception last = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            try { server.ping(); return; }
            catch (Exception unavailable) { last = unavailable; Thread.sleep(100); }
        }
        throw last;
    }
}
