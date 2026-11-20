package platform.api.theme;

import java.util.List;
import platform.api.plugin.PluginId;
import platform.api.service.Service;

/** Registry for complete themes and deterministic partial overlays. */
public interface ThemeRegistry extends Service {
    AutoCloseable register(ThemeProvider provider);
    void activate(PluginId themeId);
    PluginId activeTheme();
    ThemeTokens current();
    List<ThemeProvider> providers();
    AutoCloseable onChange(Runnable listener);
}
