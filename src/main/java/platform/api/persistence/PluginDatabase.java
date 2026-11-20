package platform.api.persistence;

import java.util.List;
import platform.api.plugin.PluginId;
import platform.api.service.Service;

/** Public persistence boundary shared by embedded and client/server Derby profiles. */
public interface PluginDatabase extends Service {
    void migrate(PluginId owner, List<PluginMigration> migrations) throws Exception;
	void baselineIfTableExists(PluginId owner, PluginMigration migration, String tableName) throws Exception;
    <T> T read(SqlWork<T> work) throws Exception;
    <T> T write(SqlWork<T> work) throws Exception;
}
