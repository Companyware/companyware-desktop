package plugins.community.mbduscustomer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;
import platform.api.ui.DesktopShell;
import platform.api.ui.PlatformServiceIds;
import plugins.community.mbduscustomer.view.CustomerPanel;
import plugins.community.mbduscustomer.application.CustomerService;
import plugins.community.mbduscustomer.infrastructure.JdbcCustomerService;

/** Customer-management reference plugin implemented exclusively against SDK v1. */
public final class Mbduscustomer implements Plugin {
    @Override public void initialize(final PluginContext context) throws Exception {
        final PluginDatabase database = context.database();
        PluginMigration create = new PluginMigration(1, "create customer table", migration("createCustomer.sql"));
        database.baselineIfTableExists(context.descriptor().getId(), create, "Customer");
        database.migrate(context.descriptor().getId(), Collections.singletonList(create));
        final CustomerService customers = new JdbcCustomerService(database);
        context.addLocalizedView("customer.view", "customer", 110,
                () -> new CustomerPanel(customers, context.messages()));
        context.addLocalizedNavigation("customer.navigation", "customer", 110,
                "customer.view");
        context.addLocalizedMenuItem("customer.menu", "customer", 110, "settings", () -> {
            DesktopShell shell = context.find(PlatformServiceIds.DESKTOP_SHELL)
                    .orElseThrow(() -> new IllegalStateException("Desktop shell is unavailable"));
            shell.showView(new CustomerPanel(customers, context.messages()));
        });
    }

    private String migration(String filename) throws IOException {
        String path = "plugins/community/mbduscustomer/migration/" + filename;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing migration resource: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
        }
    }
}
