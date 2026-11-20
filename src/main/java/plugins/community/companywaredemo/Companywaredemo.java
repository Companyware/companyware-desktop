package plugins.community.companywaredemo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;
import platform.api.ui.DesktopShell;
import platform.api.ui.PlatformServiceIds;
import plugins.community.companywaredemo.view.DemoPanel;
import plugins.community.companywaredemo.application.DemoOverviewService;
import plugins.community.companywaredemo.infrastructure.JdbcDemoOverviewService;

/** Minimal database-backed reference plugin implemented only against Plugin SDK v1. */
public final class Companywaredemo implements Plugin {
    @Override public void initialize(final PluginContext context) throws Exception {
        final PluginDatabase database = context.database();
		PluginMigration create = new PluginMigration(1, "create demo table", migration("createTable.sql"),
				"6fbea5003ff21db7d9b86a8e1221838b43bc3089687fbf4321b9d74892b502da",
				"b6d8cb6a5a970344c5c19505b51fca8d5f95f599246ddc93db90748384f8949c");
		database.baselineIfTableExists(context.descriptor().getId(), create, "Demo");
        database.migrate(context.descriptor().getId(), Arrays.asList(create,
                new PluginMigration(2, "insert demo sample", migration("insertData.sql"),
						"f5a3eb6f7456b1c612c5ed866fef28bd215ec71d8134c399fcea3996862fd5d9",
						"026ddc0f587f2175a9e3416b61a6c25146f6e24b59272e7fc0bb377d67c5c4d3")));

        final DemoOverviewService overview = new JdbcDemoOverviewService(database);
        context.addLocalizedView("overview.view", "demo.overview", 100,
                () -> new DemoPanel(overview, context.messages()));
        context.addLocalizedNavigation("overview.navigation", "demo.overview", 100,
                "overview.view");
        context.addLocalizedMenuItem("overview.menu", "demo.overview", 100, "settings", () -> {
            DesktopShell shell = context.find(PlatformServiceIds.DESKTOP_SHELL)
                    .orElseThrow(() -> new IllegalStateException("Desktop shell is unavailable"));
            shell.showView(new DemoPanel(overview, context.messages()));
        });
    }

    private String migration(String filename) throws IOException {
        String path = "plugins/community/companywaredemo/migration/" + filename;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing migration resource: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
