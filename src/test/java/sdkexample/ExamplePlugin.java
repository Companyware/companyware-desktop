package sdkexample;

import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;
import platform.api.service.Service;
import platform.api.service.ServiceKey;
import javax.swing.JLabel;

/** Compile-only proof that a plugin can depend exclusively on the public SDK. */
public final class ExamplePlugin implements Plugin {
    public static final ServiceKey<GreetingService> GREETING =
            ServiceKey.of("example.greeting", GreetingService.class);

    @Override
    public void initialize(PluginContext context) {
        context.register(GREETING, new GreetingService());
        context.addView("welcome", "Welcome", 100, () -> new JLabel("Hello from the SDK"));
        context.addNavigation("welcome.navigation", "Welcome", 100, "welcome");
        context.addMenuItem("welcome.menu", "Welcome", 100, "help", () -> { });
        context.addSettingsPage("welcome.settings", "Welcome settings", 100, () -> new JLabel("Settings"));
    }

    public static final class GreetingService implements Service {
        public String greeting() { return "Hello"; }
    }
}
