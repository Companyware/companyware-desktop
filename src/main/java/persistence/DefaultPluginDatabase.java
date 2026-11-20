package persistence;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import core.HibernateUtils;
import persistence.migration.Migration;
import persistence.migration.MigrationRunner;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.persistence.SqlWork;
import platform.api.plugin.PluginId;

/** Platform-owned JDBC/migration adapter; plugins never depend on Hibernate internals. */
public final class DefaultPluginDatabase implements PluginDatabase {
	@Override public void baselineIfTableExists(PluginId owner, PluginMigration migration,
			String tableName) throws Exception {
		withConnection(connection -> {
			try (ResultSet tables = connection.getMetaData().getTables(null, "APP",
					tableName.toUpperCase(java.util.Locale.ROOT), new String[] { "TABLE" })) {
				if (tables.next()) new MigrationRunner().baseline(connection, new Migration(owner,
						migration.version(), migration.description(), migration.sql(),
						migration.acceptedPreviousChecksums()));
			}
			return null;
		}, false);
	}
    @Override public void migrate(PluginId owner, List<PluginMigration> migrations) throws Exception {
        withConnection(connection -> {
            List<Migration> internal = new ArrayList<Migration>();
            for (PluginMigration migration : migrations) internal.add(new Migration(owner,
                    migration.version(), migration.description(), migration.sql(),
                    migration.acceptedPreviousChecksums()));
            new MigrationRunner().migrate(connection, internal);
            return null;
        }, false);
    }

    @Override public <T> T read(SqlWork<T> work) throws Exception { return withConnection(work, false); }
    @Override public <T> T write(SqlWork<T> work) throws Exception { return withConnection(work, true); }

    private <T> T withConnection(SqlWork<T> work, boolean transactional) throws Exception {
        SessionFactory factory = HibernateUtils.getSessionFactory();
        ConnectionProvider provider = factory.getSessionFactoryOptions().getServiceRegistry()
                .getService(ConnectionProvider.class);
        return JdbcResources.withConnection(provider, work, transactional);
    }
}
