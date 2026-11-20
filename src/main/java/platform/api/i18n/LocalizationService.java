package platform.api.i18n;

import java.util.List;
import java.util.Locale;
import platform.api.service.Service;

/** Runtime localization shared by the platform and all plugins. */
public interface LocalizationService extends Service {
    Locale locale();
    void setLocale(Locale locale);
    List<Locale> supportedLocales();
    PluginMessages messages(String baseName);
    AutoCloseable onChange(Runnable listener);
}
