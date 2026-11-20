package desktop.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import java.awt.Color;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import org.junit.Test;
import platform.api.plugin.PluginId;
import platform.api.theme.ThemeColor;
import platform.api.theme.ThemeFont;
import platform.api.theme.ThemeProvider;
import platform.api.theme.ThemeSpacing;
import platform.api.theme.ThemeTokens;
import platform.api.version.SemanticVersion;
import platform.api.ui.AccessRequirement;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import plugins.community.darktheme.DarkThemeProvider;
import core.ui.CompanywareTheme;

public class UiRegistryTest {
    @Test public void deferredEdtWorkRunsAfterTheCurrentSwingCallback() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        SwingUtilities.invokeAndWait(() -> {
            SwingEdt.runLater(() -> ran.set(true));
            assertFalse(ran.get());
        });
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue(ran.get());
    }

    @Test public void menuTreeUsesOneSemanticPaletteEvenBeforePopupsAreShown() {
        ThemeTokens tokens = new DarkThemeProvider().tokens();
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        JMenuItem disabled = new JMenuItem("Unavailable");
        disabled.setEnabled(false);
        file.add(exit);
        file.add(disabled);
        bar.add(file);

        DefaultThemeRegistry.applyMenuColors(bar, tokens);

        assertEquals(tokens.color(ThemeColor.SURFACE), bar.getBackground());
        assertEquals(tokens.color(ThemeColor.SURFACE), file.getPopupMenu().getBackground());
        assertEquals(tokens.color(ThemeColor.TEXT), file.getForeground());
        assertEquals(tokens.color(ThemeColor.TEXT), exit.getForeground());
        assertEquals(tokens.color(ThemeColor.MUTED_TEXT), disabled.getForeground());
    }

    @Test public void primaryLoginButtonRetainsReadableContrastAcrossUiRefresh() {
        DefaultThemeRegistry themes = new DefaultThemeRegistry(new MemoryThemeSelectionStore());
        JButton login = new JButton("Login");
        CompanywareTheme.primaryButton(login);
        Color expectedBackground = CompanywareTheme.primary();

        login.updateUI();

        assertEquals(expectedBackground, login.getBackground());
        assertEquals(CompanywareTheme.TEXT, login.getForeground());
        assertTrue(contrast(login.getForeground(), login.getBackground()) >= 4.5d);
        assertEquals(core.ui.CompanywareThemeProvider.ID, themes.activeTheme());
    }

    @Test public void contributionsAreOrderedAndChangesRunOnEdt() throws Exception {
        DefaultUiRegistry registry = new DefaultUiRegistry();
        AtomicBoolean notifiedOnEdt = new AtomicBoolean(false);
        AutoCloseable listener = registry.onChange(() -> notifiedOnEdt.set(SwingUtilities.isEventDispatchThread()));
        AutoCloseable later = registry.register(view("b.plugin", "later", 20));
        registry.register(view("a.plugin", "first", 10));
        assertTrue(notifiedOnEdt.get());
        assertEquals("first", registry.contributions(UiContributionType.VIEW).get(0).id());
        later.close();
        assertEquals(1, registry.contributions(UiContributionType.VIEW).size());
        listener.close();
    }

    @Test public void overlaysUsePriorityAndCoreFallbackCannotBeRemoved() throws Exception {
        DefaultThemeRegistry themes = new DefaultThemeRegistry();
        ThemeProvider low = overlay("theme.low", 10, Color.RED);
        ThemeProvider high = overlay("theme.high", 20, Color.BLUE);
        AutoCloseable lowRegistration = themes.register(low);
        AutoCloseable highRegistration = themes.register(high);
        assertEquals(Color.BLUE, themes.current().color(ThemeColor.ACCENT));
        highRegistration.close();
        assertEquals(Color.RED, themes.current().color(ThemeColor.ACCENT));
        lowRegistration.close();
        assertEquals(core.ui.CompanywareTheme.VIOLET, themes.current().color(ThemeColor.ACCENT));
        assertEquals(core.ui.CompanywareThemeProvider.ID, themes.activeTheme());
    }

    @Test public void darkThemeIsCompleteReadablePersistentAndFallsBackWhenRemoved() throws Exception {
        MemoryThemeSelectionStore store = new MemoryThemeSelectionStore();
        DefaultThemeRegistry themes = new DefaultThemeRegistry(store);
        AutoCloseable dark = themes.register(new DarkThemeProvider());
        themes.activate(DarkThemeProvider.ID);
        assertEquals(DarkThemeProvider.ID, themes.activeTheme());
        assertEquals(DarkThemeProvider.ID.value(), store.value);
        for (ThemeColor color : ThemeColor.values()) assertTrue(themes.current().color(color) != null);
        for (ThemeFont font : ThemeFont.values()) assertTrue(themes.current().font(font) != null);
        for (ThemeSpacing spacing : ThemeSpacing.values()) assertTrue(themes.current().spacing(spacing) >= 0);
        assertTrue("Dark text contrast must be at least WCAG AA", contrast(
                themes.current().color(ThemeColor.TEXT), themes.current().color(ThemeColor.CANVAS)) >= 4.5d);
        assertNotEquals(core.ui.CompanywareThemeProvider.ID, themes.activeTheme());
        assertEquals(themes.current().color(ThemeColor.TEXT), UIManager.getColor("Menu.foreground"));
        assertEquals(themes.current().color(ThemeColor.TEXT), UIManager.getColor("MenuItem.foreground"));
        assertEquals(themes.current().color(ThemeColor.TEXT), UIManager.getColor("MenuItem.selectionForeground"));
        JMenu menu = new JMenu("Settings");
        JMenuItem item = new JMenuItem("Plugins");
        assertEquals(themes.current().color(ThemeColor.TEXT), menu.getForeground());
        assertEquals(themes.current().color(ThemeColor.TEXT), item.getForeground());

        JPanel panelCreatedUnderDark = new JPanel();
        assertTrue(panelCreatedUnderDark.getBackground() instanceof UIResource);
        themes.activate(core.ui.CompanywareThemeProvider.ID);
        panelCreatedUnderDark.updateUI();
        assertEquals(themes.current().color(ThemeColor.CANVAS), panelCreatedUnderDark.getBackground());

        themes.activate(DarkThemeProvider.ID);
        dark.close();
        assertEquals(core.ui.CompanywareThemeProvider.ID, themes.activeTheme());

        DefaultThemeRegistry restored = new DefaultThemeRegistry(store);
        restored.register(new DarkThemeProvider());
        assertEquals(DarkThemeProvider.ID, restored.activeTheme());
    }

    private double contrast(Color first, Color second) {
        double bright = Math.max(luminance(first), luminance(second));
        double dark = Math.min(luminance(first), luminance(second));
        return (bright + .05d) / (dark + .05d);
    }

    private double luminance(Color color) {
        double[] channels = { color.getRed() / 255d, color.getGreen() / 255d, color.getBlue() / 255d };
        double result = 0d;
        double[] weights = { .2126d, .7152d, .0722d };
        for (int index = 0; index < channels.length; index++) {
            double channel = channels[index];
            channel = channel <= .03928d ? channel / 12.92d : Math.pow((channel + .055d) / 1.055d, 2.4d);
            result += channel * weights[index];
        }
        return result;
    }

    private static final class MemoryThemeSelectionStore implements ThemeSelectionStore {
        private String value;
        @Override public String load() { return value; }
        @Override public void save(String themeId) { value = themeId; }
    }

    private UiContributionSpec view(String owner, String id, int order) {
        return new UiContributionSpec(PluginId.of(owner), id, id, UiContributionType.VIEW, order,
                null, AccessRequirement.AUTHENTICATED, () -> new JLabel(id), null);
    }

    private ThemeProvider overlay(final String id, final int priority, final Color accent) {
        return new ThemeProvider() {
            public PluginId id() { return PluginId.of(id); }
            public String displayName() { return id; }
            public SemanticVersion version() { return SemanticVersion.parse("1.0.0"); }
            public boolean overlay() { return true; }
            public int priority() { return priority; }
            public ThemeTokens tokens() { return new ThemeTokens() {
                public Color color(ThemeColor token) { return token == ThemeColor.ACCENT ? accent : null; }
                public Font font(ThemeFont token) { return null; }
                public int spacing(ThemeSpacing token) { return -1; }
            }; }
        };
    }
}
