package core.security;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import core.HibernateUtils;
import persistence.migration.Migration;
import persistence.migration.MigrationRunner;
import platform.api.plugin.PluginId;

/** Upgrades pre-CW-D-007 user tables before authentication queries run. */
public final class SecuritySchemaMigration {
    private static final PluginId OWNER = PluginId.of("companyware.identity");
    private SecuritySchemaMigration() {}

    public static void migrate() throws SQLException {
        SessionFactory factory = HibernateUtils.getSessionFactory();
        ConnectionProvider provider = factory.getSessionFactoryOptions().getServiceRegistry().getService(ConnectionProvider.class);
        Connection connection = provider.getConnection();
        try { migrate(connection); }
        finally { provider.closeConnection(connection); }
    }

    public static void migrate(Connection connection) throws SQLException {
        if (!tableExists(connection, "USERS")) return;
        Migration required = new Migration(OWNER, 1, "add credential change requirement",
                "ALTER TABLE Users ADD COLUMN password_change_required BOOLEAN DEFAULT TRUE");
        Migration roles = new Migration(OWNER, 2, "add user roles",
                "ALTER TABLE Users ADD COLUMN roles VARCHAR(500) DEFAULT 'USER'");
        Migration permissions = new Migration(OWNER, 3, "add user permissions",
                "ALTER TABLE Users ADD COLUMN permissions VARCHAR(2000) DEFAULT ''");
        Migration administrator = new Migration(OWNER, 4, "grant built-in administrator privileges",
                "UPDATE Users SET roles='ADMIN', permissions='*' WHERE username='companyware'");
        Migration locale = new Migration(OWNER, 5, "add user locale preference",
                "ALTER TABLE Users ADD COLUMN locale VARCHAR(16) DEFAULT 'de'");
        Migration recovery = new Migration(OWNER, 6, "add one-time account recovery keys",
                "CREATE TABLE AccountRecoveryKeys (selector VARCHAR(64) PRIMARY KEY, username VARCHAR(100) NOT NULL, secret_hash VARCHAR(100) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, failures INTEGER NOT NULL DEFAULT 0, locked_until TIMESTAMP, issued_at TIMESTAMP NOT NULL, consumed_at TIMESTAMP)");
        Migration emailVerified = new Migration(OWNER, 7, "track verified profile e-mail addresses",
                "ALTER TABLE Users ADD COLUMN email_verified BOOLEAN DEFAULT FALSE");
        Migration emailVerifiedAt = new Migration(OWNER, 8, "track profile e-mail verification time",
                "ALTER TABLE Users ADD COLUMN email_verified_at TIMESTAMP");
        Migration emailTokens = new Migration(OWNER, 9, "add expiring e-mail verification and recovery tokens",
                "CREATE TABLE AccountEmailTokens (selector VARCHAR(64) PRIMARY KEY, purpose VARCHAR(16) NOT NULL, username VARCHAR(100) NOT NULL, email VARCHAR(320) NOT NULL, secret_hash VARCHAR(100) NOT NULL, expires_at TIMESTAMP NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, failures INTEGER NOT NULL DEFAULT 0, locked_until TIMESTAMP, issued_at TIMESTAMP NOT NULL, consumed_at TIMESTAMP)");
        MigrationRunner runner = new MigrationRunner();
        baselineExisting(connection, runner, required, "PASSWORD_CHANGE_REQUIRED");
        baselineExisting(connection, runner, roles, "ROLES");
        baselineExisting(connection, runner, permissions, "PERMISSIONS");
		baselineExisting(connection, runner, locale, "LOCALE");
		if (tableExists(connection, "ACCOUNTRECOVERYKEYS")) runner.baseline(connection, recovery);
		baselineExisting(connection, runner, emailVerified, "EMAIL_VERIFIED");
		baselineExisting(connection, runner, emailVerifiedAt, "EMAIL_VERIFIED_AT");
		if (tableExists(connection, "ACCOUNTEMAILTOKENS")) runner.baseline(connection, emailTokens);
		runner.migrate(connection, Arrays.asList(required, roles, permissions, administrator, locale, recovery,
				emailVerified, emailVerifiedAt, emailTokens));
    }

    private static void baselineExisting(Connection connection, MigrationRunner runner,
            Migration migration, String column) throws SQLException {
        if (columnExists(connection, "USERS", column)) runner.baseline(connection, migration);
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet result = connection.getMetaData().getTables(null, "APP", table, new String[] {"TABLE"})) {
            return result.next();
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet result = connection.getMetaData().getColumns(null, "APP", table, column)) {
            return result.next();
        }
    }
}
