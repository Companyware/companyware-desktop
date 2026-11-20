package core.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import platform.api.i18n.LocalizationService;
import platform.api.i18n.PluginMessages;

/** English-fallback ResourceBundle localization with EDT-safe live updates. */
public final class DefaultLocalizationService implements LocalizationService {
	private static final ResourceBundle.Control UTF8_CONTROL = new Utf8Control();
    private static final List<Locale> SUPPORTED = Collections.unmodifiableList(Arrays.asList(
            Locale.GERMAN, Locale.ENGLISH, Locale.FRENCH, new Locale("es")));
    private final List<Runnable> listeners = new CopyOnWriteArrayList<Runnable>();
    private volatile Locale locale = Locale.GERMAN;

    @Override public Locale locale() { return locale; }
    @Override public List<Locale> supportedLocales() { return SUPPORTED; }

    @Override public void setLocale(Locale value) {
        Locale normalized = normalize(value);
        if (normalized.equals(locale)) return;
        locale = normalized;
        Locale.setDefault(normalized);
        Runnable notification = new Runnable() {
            @Override public void run() { for (Runnable listener : listeners) listener.run(); }
        };
        if (SwingUtilities.isEventDispatchThread()) notification.run(); else SwingUtilities.invokeLater(notification);
    }

    @Override public PluginMessages messages(final String baseName) {
        return new PluginMessages() {
            @Override public Locale locale() { return DefaultLocalizationService.this.locale(); }
            @Override public String text(String key, Object... arguments) {
                String pattern = lookup(baseName, key, locale());
                if (arguments == null || arguments.length == 0) return pattern;
                return new MessageFormat(pattern, locale()).format(arguments);
            }
        };
    }

    @Override public AutoCloseable onChange(final Runnable listener) {
        listeners.add(listener);
        return new AutoCloseable() { @Override public void close() { listeners.remove(listener); } };
    }

    private static String lookup(String baseName, String key, Locale requested) {
		try { return ResourceBundle.getBundle(baseName, requested, UTF8_CONTROL).getString(key); }
        catch (MissingResourceException missing) {
			try { return ResourceBundle.getBundle(baseName, Locale.ENGLISH, UTF8_CONTROL).getString(key); }
            catch (MissingResourceException absentInEnglish) { return key; }
        }
    }

	private static final class Utf8Control extends ResourceBundle.Control {
		@Override public Locale getFallbackLocale(String baseName, Locale locale) { return null; }
		@Override public ResourceBundle newBundle(String baseName, Locale locale, String format,
				ClassLoader loader, boolean reload) throws IOException, IllegalAccessException, InstantiationException {
			if (!"java.properties".equals(format)) return super.newBundle(baseName, locale, format, loader, reload);
			String resourceName = toResourceName(toBundleName(baseName, locale), "properties");
			InputStream stream = loader.getResourceAsStream(resourceName);
			if (stream == null) return null;
			try { return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8)); }
			finally { stream.close(); }
		}
	}

    private static Locale normalize(Locale value) {
        if (value != null) for (Locale supported : SUPPORTED)
            if (supported.getLanguage().equals(value.getLanguage())) return supported;
        return Locale.ENGLISH;
    }
}
