package plugins.community.darktheme;

import java.awt.Color;
import java.awt.Font;
import platform.api.plugin.PluginId;
import platform.api.theme.ThemeColor;
import platform.api.theme.ThemeFont;
import platform.api.theme.ThemeProvider;
import platform.api.theme.ThemeSpacing;
import platform.api.theme.ThemeTokens;
import platform.api.version.SemanticVersion;

/** Complete dark palette; no shell implementation dependency is required. */
public final class DarkThemeProvider implements ThemeProvider {
    public static final PluginId ID = PluginId.of("companyware.theme.dark");
    private static final Font BODY = new Font("Segoe UI", Font.PLAIN, 15);

    @Override public PluginId id() { return ID; }
    @Override public String displayName() { return "Companyware Dark"; }
    @Override public SemanticVersion version() { return SemanticVersion.parse("1.0.0"); }
    @Override public boolean overlay() { return false; }
    @Override public ThemeTokens tokens() { return new ThemeTokens() {
        @Override public Color color(ThemeColor token) {
            switch (token) {
                case CANVAS: return new Color(24, 27, 38);
                case SURFACE: return new Color(34, 38, 52);
                case SIDEBAR: return new Color(29, 32, 46);
                case TEXT: return new Color(239, 241, 248);
                case MUTED_TEXT: return new Color(178, 184, 204);
                case BORDER: return new Color(70, 76, 98);
                case PRIMARY: return new Color(91, 164, 230);
                case ACCENT: return new Color(158, 137, 235);
                case SUCCESS: return new Color(72, 199, 142);
                case WARNING: return new Color(245, 183, 74);
                case ERROR: return new Color(245, 101, 113);
                default: throw new IllegalArgumentException(token.name());
            }
        }
        @Override public Font font(ThemeFont token) {
            switch (token) {
                case LABEL: return BODY.deriveFont(Font.BOLD, 13f);
                case TITLE: return BODY.deriveFont(Font.BOLD, 30f);
                default: return BODY;
            }
        }
        @Override public int spacing(ThemeSpacing token) {
            switch (token) {
                case EXTRA_SMALL: return 4;
                case SMALL: return 8;
                case MEDIUM: return 16;
                case LARGE: return 24;
                default: return 32;
            }
        }
    }; }
}
