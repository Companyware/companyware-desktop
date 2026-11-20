package plugins.community.darktheme;

import platform.api.plugin.Plugin;
import platform.api.plugin.PluginContext;

/** Optional dark Companyware theme implemented only against the public SDK. */
public final class Darktheme implements Plugin {
    @Override public void initialize(PluginContext context) {
        context.closeOnStop(context.themes().register(new DarkThemeProvider()));
    }
}
