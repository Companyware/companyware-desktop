package platform.api.i18n;

import java.util.Locale;

/** Plugin-scoped localized messages. */
public interface PluginMessages {
    Locale locale();
    String text(String key, Object... arguments);
}
