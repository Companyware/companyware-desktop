package plugins.community.helloworld.infrastructure;

import java.sql.ResultSet;
import java.sql.Statement;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.helloworld.application.GreetingService;

/** Local persistence adapter. A remote adapter can implement the same application port later. */
public final class JdbcGreetingService implements GreetingService {
    private final PluginDatabase database;
    public JdbcGreetingService(PluginDatabase database) { this.database = database; }

    @Override public ApplicationResult<String> greeting() {
        try {
            String value = database.read(connection -> {
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT message FROM HelloWorld ORDER BY id FETCH FIRST ROW ONLY")) {
                    return result.next() ? result.getString(1) : "Hello World!";
                }
            });
            return ApplicationResult.success(value);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Greeting is unavailable");
        }
    }
}
