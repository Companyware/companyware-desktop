package desktop.ui;

import java.util.prefs.Preferences;

final class PreferencesThemeSelectionStore implements ThemeSelectionStore {
    private static final String KEY = "active-theme";
    private final Preferences preferences = Preferences.userNodeForPackage(DefaultThemeRegistry.class);

    @Override public String load() {
        try { return preferences.get(KEY, null); }
        catch (SecurityException unavailable) { return null; }
    }

    @Override public void save(String themeId) {
        try { preferences.put(KEY, themeId); }
        catch (SecurityException unavailable) { /* Runtime selection still remains usable. */ }
    }
}
