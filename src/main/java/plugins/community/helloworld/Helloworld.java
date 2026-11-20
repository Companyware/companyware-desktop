package plugins.community.helloworld;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PluginMigration;
import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;
import plugins.community.helloworld.view.HelloWorldPanel;
import plugins.community.helloworld.application.GreetingService;
import plugins.community.helloworld.infrastructure.JdbcGreetingService;

/** Small, complete SDK example with its own schema, navigation and Swing view. */
public final class Helloworld implements Plugin {
    @Override public void initialize(final PluginContext context) throws Exception {
        final PluginDatabase database = context.database();
        database.migrate(context.descriptor().getId(), Arrays.asList(
                new PluginMigration(1, "create HelloWorld table",
                        migration("createTable.sql")),
                new PluginMigration(2, "insert HelloWorld greeting",
                        migration("insertGreeting.sql"))));

        final GreetingService greetings = new JdbcGreetingService(database);
        context.addLocalizedView("hello-world.view", "hello.title", 120,
                () -> new HelloWorldPanel(greetings, context.themes().current()));
        context.addLocalizedNavigation("hello-world.navigation", "hello.title", 120,
                "hello-world.view");
    }

    private String migration(String filename) throws IOException {
        String path = "plugins/community/helloworld/migration/" + filename;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing migration resource: " + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            return new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
        }
    }
}
