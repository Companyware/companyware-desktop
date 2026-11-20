package plugins.community.companywaredemo.infrastructure;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.companywaredemo.application.DemoOverviewService;
import plugins.community.companywaredemo.application.DemoRow;

/** Derby/JDBC adapter for the local desktop deployment. */
public final class JdbcDemoOverviewService implements DemoOverviewService {
    private final PluginDatabase database;
    public JdbcDemoOverviewService(PluginDatabase database) { this.database = database; }

    @Override public ApplicationResult<List<DemoRow>> listRows() {
        try {
            return ApplicationResult.success(database.read(connection -> {
                List<DemoRow> values = new ArrayList<DemoRow>();
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT salutation, name, firstname, street, streetnumber, zipcode, city, country FROM Demo ORDER BY id")) {
                    while (result.next()) {
                        String[] row = new String[8];
                        for (int index = 0; index < row.length; index++) row[index] = result.getString(index + 1);
                        values.add(new DemoRow(row));
                    }
                }
                return values;
            }));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Demo overview is unavailable");
        }
    }
}
