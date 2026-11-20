package core.ui;

import platform.api.plugin.PluginId;
import platform.api.theme.ThemeColor;
import platform.api.theme.ThemeFont;
import platform.api.theme.ThemeProvider;
import platform.api.theme.ThemeSpacing;
import platform.api.theme.ThemeTokens;
import platform.api.version.SemanticVersion;

/** Protected fallback exposing the existing light Companyware design through the public API. */
public final class CompanywareThemeProvider implements ThemeProvider {
    public static final PluginId ID = PluginId.of("companyware.light");
    public PluginId id() { return ID; }
    public String displayName() { return "Companyware Light"; }
    public SemanticVersion version() { return SemanticVersion.parse("1.0.0"); }
    public boolean overlay() { return false; }
    public ThemeTokens tokens() { return new ThemeTokens() {
        public java.awt.Color color(ThemeColor token) {
            switch (token) {
                case CANVAS: return CompanywareTheme.CANVAS; case SURFACE: return CompanywareTheme.SURFACE;
                case SIDEBAR: return CompanywareTheme.SIDEBAR; case TEXT: return CompanywareTheme.TEXT;
                case MUTED_TEXT: return CompanywareTheme.MUTED; case BORDER: return CompanywareTheme.BORDER;
                case PRIMARY: return CompanywareTheme.ROYAL; case ACCENT: return CompanywareTheme.VIOLET;
                case SUCCESS: return CompanywareTheme.SUCCESS; case WARNING: return new java.awt.Color(190, 125, 25);
                case ERROR: return new java.awt.Color(200, 45, 55); default: throw new IllegalArgumentException(token.name());
            }
        }
        public java.awt.Font font(ThemeFont token) {
            switch (token) { case LABEL: return CompanywareTheme.LABEL; case TITLE: return CompanywareTheme.TITLE;
                default: return CompanywareTheme.BODY; }
        }
        public int spacing(ThemeSpacing token) {
            switch (token) { case EXTRA_SMALL: return 4; case SMALL: return 8; case MEDIUM: return 16;
                case LARGE: return 24; default: return 32; }
        }
    }; }
}
