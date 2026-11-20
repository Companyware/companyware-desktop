package persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.hibernate.cfg.Configuration;

/** One validated source of Derby connection settings for Hibernate and migrations. */
public final class PersistenceSettings {
    private static final String PREFIX = "companyware.persistence.";
    private final PersistenceProfile profile;
    private final String driverClass;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final Path embeddedPath;

    private PersistenceSettings(PersistenceProfile profile, String driverClass, String jdbcUrl,
            String username, String password, Path embeddedPath) {
        this.profile = profile;
        this.driverClass = driverClass;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.embeddedPath = embeddedPath;
    }

    public static PersistenceSettings load() {
        Properties defaults = new Properties();
        try (InputStream input = PersistenceSettings.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) defaults.load(input);
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to read persistence defaults", failure);
        }
        return from(defaults);
    }

    public static PersistenceSettings from(Properties defaults) {
        PersistenceProfile profile = PersistenceProfile.parse(value(defaults, "profile", "embedded"));
        String username = value(defaults, "username", "app");
        String password = value(defaults, "password", "ap");
        if (profile == PersistenceProfile.EMBEDDED) {
            Path path = Paths.get(value(defaults, "embedded.path", "data/companyware")).normalize();
            if (path.toString().trim().isEmpty()) throw new IllegalArgumentException("Embedded database path is empty");
            return new PersistenceSettings(profile, "org.apache.derby.jdbc.EmbeddedDriver",
                    "jdbc:derby:" + path.toString().replace('\\', '/') + ";create=true", username, password, path);
        }
        String host = value(defaults, "host", "localhost");
        int port;
        try { port = Integer.parseInt(value(defaults, "port", "1527")); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Invalid Derby port", invalid); }
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid Derby port: " + port);
        String database = value(defaults, "database", "companyware");
        if (!host.matches("[A-Za-z0-9._-]+") || !database.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid Derby host or database name");
        }
        return new PersistenceSettings(profile, "org.apache.derby.jdbc.ClientDriver",
                "jdbc:derby://" + host + ":" + port + "/" + database + ";create=true", username, password, null);
    }

    private static String value(Properties defaults, String name, String fallback) {
        String key = PREFIX + name;
        String system = System.getProperty(key);
        String result = system != null ? system : defaults.getProperty(key, fallback);
        if (result == null || result.trim().isEmpty()) throw new IllegalArgumentException("Missing setting: " + key);
        return result.trim();
    }

    public void applyTo(Configuration configuration) {
        configuration.setProperty("hibernate.connection.driver_class", driverClass);
        configuration.setProperty("hibernate.connection.url", jdbcUrl);
        configuration.setProperty("hibernate.connection.username", username);
        configuration.setProperty("hibernate.connection.password", password);
    }

    public PersistenceProfile getProfile() { return profile; }
    public String getDriverClass() { return driverClass; }
    public String getJdbcUrl() { return jdbcUrl; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Path getEmbeddedPath() { return embeddedPath; }
}
