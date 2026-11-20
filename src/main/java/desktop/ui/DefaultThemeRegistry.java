package desktop.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import core.ui.CompanywareThemeProvider;
import platform.api.plugin.PluginId;
import platform.api.theme.ThemeColor;
import platform.api.theme.ThemeFont;
import platform.api.theme.ThemeProvider;
import platform.api.theme.ThemeRegistry;
import platform.api.theme.ThemeSpacing;
import platform.api.theme.ThemeTokens;

/** Deterministic base-theme and overlay registry with protected Core fallback. */
public final class DefaultThemeRegistry implements ThemeRegistry {
    private final Map<PluginId, ThemeProvider> providers = new LinkedHashMap<PluginId, ThemeProvider>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();
    private final ThemeSelectionStore selectionStore;
    private String preferredTheme;
    private PluginId active = CompanywareThemeProvider.ID;

    public DefaultThemeRegistry() {
        this(new PreferencesThemeSelectionStore());
    }

    DefaultThemeRegistry(ThemeSelectionStore selectionStore) {
        this.selectionStore = selectionStore;
        this.preferredTheme = selectionStore.load();
        providers.put(active, new CompanywareThemeProvider());
        SwingEdt.run(() -> applyAndNotify());
    }

    @Override public AutoCloseable register(final ThemeProvider provider) {
        SwingEdt.run(() -> {
            if (providers.containsKey(provider.id())) throw new IllegalStateException("Duplicate theme: " + provider.id());
            providers.put(provider.id(), provider);
            if (!provider.overlay() && provider.id().value().equals(preferredTheme)) active = provider.id();
            applyAndNotify();
        });
        final AtomicBoolean open = new AtomicBoolean(true);
        return () -> {
            if (!open.compareAndSet(true, false)) return;
            SwingEdt.run(() -> { if (!CompanywareThemeProvider.ID.equals(provider.id())) { providers.remove(provider.id());
                if (active.equals(provider.id())) active = CompanywareThemeProvider.ID; applyAndNotify(); }});
        };
    }

    @Override public void activate(final PluginId themeId) { SwingEdt.run(() -> {
        ThemeProvider provider = providers.get(themeId);
        if (provider == null || provider.overlay()) throw new IllegalArgumentException("Unknown base theme: " + themeId);
        active = themeId;
        preferredTheme = themeId.value();
        selectionStore.save(themeId.value());
        applyAndNotify();
    }); }
    @Override public PluginId activeTheme() { return SwingEdt.call(() -> active); }
    @Override public ThemeTokens current() { return SwingEdt.call(() -> resolved()); }
    @Override public List<ThemeProvider> providers() { return SwingEdt.call(() -> {
        List<ThemeProvider> result = orderedProviders(); return Collections.unmodifiableList(result); }); }
    @Override public AutoCloseable onChange(final Runnable listener) {
        SwingEdt.run(() -> listeners.add(listener)); final AtomicBoolean open = new AtomicBoolean(true);
        return () -> { if (open.compareAndSet(true, false)) SwingEdt.run(() -> listeners.remove(listener)); };
    }

    private ThemeTokens resolved() {
        final ThemeTokens base = providers.get(active).tokens();
        final List<ThemeProvider> overlays = new ArrayList<ThemeProvider>();
        for (ThemeProvider provider : orderedProviders()) if (provider.overlay()) overlays.add(provider);
        return new ThemeTokens() {
            public java.awt.Color color(ThemeColor token) { java.awt.Color value = base.color(token);
                for (ThemeProvider overlay : overlays) { java.awt.Color candidate = overlay.tokens().color(token); if (candidate != null) value = candidate; } return value; }
            public java.awt.Font font(ThemeFont token) { java.awt.Font value = base.font(token);
                for (ThemeProvider overlay : overlays) { java.awt.Font candidate = overlay.tokens().font(token); if (candidate != null) value = candidate; } return value; }
            public int spacing(ThemeSpacing token) { int value = base.spacing(token);
                for (ThemeProvider overlay : overlays) { int candidate = overlay.tokens().spacing(token); if (candidate >= 0) value = candidate; } return value; }
        };
    }

    private List<ThemeProvider> orderedProviders() {
        List<ThemeProvider> result = new ArrayList<ThemeProvider>(providers.values());
        Collections.sort(result, Comparator.comparingInt(ThemeProvider::priority).thenComparing(item -> item.id().value()));
        return result;
    }
    private void applyAndNotify() {
        ThemeTokens selected = resolved();
        if (isDark(selected.color(ThemeColor.CANVAS))) FlatDarkLaf.install();
        else FlatLightLaf.install();
        ThemeTokens tokens = swingResources(selected);
        UIManager.put("Panel.background", tokens.color(ThemeColor.CANVAS));
        UIManager.put("Label.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Label.disabledForeground", tokens.color(ThemeColor.MUTED_TEXT));
        UIManager.put("Button.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("Button.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("MenuBar.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("MenuBar.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Menu.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("Menu.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Menu.selectionBackground", tokens.color(ThemeColor.ACCENT));
        UIManager.put("Menu.selectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Menu.disabledForeground", tokens.color(ThemeColor.MUTED_TEXT));
        UIManager.put("PopupMenu.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("PopupMenu.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("MenuItem.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("MenuItem.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("MenuItem.selectionBackground", tokens.color(ThemeColor.ACCENT));
        UIManager.put("MenuItem.selectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("MenuItem.disabledForeground", tokens.color(ThemeColor.MUTED_TEXT));
        UIManager.put("MenuItem.acceleratorForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("MenuItem.acceleratorSelectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("CheckBoxMenuItem.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("CheckBoxMenuItem.selectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("RadioButtonMenuItem.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("RadioButtonMenuItem.selectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("TextField.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("TextField.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("PasswordField.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("PasswordField.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("ComboBox.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("ComboBox.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Table.background", tokens.color(ThemeColor.SURFACE));
        UIManager.put("Table.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("Table.gridColor", tokens.color(ThemeColor.BORDER));
        UIManager.put("Table.selectionBackground", tokens.color(ThemeColor.ACCENT));
        UIManager.put("Table.selectionForeground", tokens.color(ThemeColor.TEXT));
        UIManager.put("TableHeader.background", tokens.color(ThemeColor.SIDEBAR));
        UIManager.put("TableHeader.foreground", tokens.color(ThemeColor.TEXT));
        UIManager.put("ScrollPane.background", tokens.color(ThemeColor.CANVAS));
        UIManager.put("Viewport.background", tokens.color(ThemeColor.CANVAS));
        UIManager.put("Component.focusColor", tokens.color(ThemeColor.PRIMARY));
        UIManager.put("Component.borderColor", tokens.color(ThemeColor.BORDER));
        UIManager.put("Companyware.canvas", tokens.color(ThemeColor.CANVAS));
        UIManager.put("Companyware.surface", tokens.color(ThemeColor.SURFACE));
        UIManager.put("Companyware.sidebar", tokens.color(ThemeColor.SIDEBAR));
        UIManager.put("Companyware.text", tokens.color(ThemeColor.TEXT));
        UIManager.put("Companyware.muted", tokens.color(ThemeColor.MUTED_TEXT));
        UIManager.put("Companyware.border", tokens.color(ThemeColor.BORDER));
        UIManager.put("Companyware.primary", tokens.color(ThemeColor.PRIMARY));
        UIManager.put("Companyware.accent", tokens.color(ThemeColor.ACCENT));
        UIManager.put("defaultFont", tokens.font(ThemeFont.BODY));
        // A theme can be activated from a JTable editor. Updating that table inside
        // its editingStopped callback re-enters the editor and invalidates its row.
        SwingEdt.runLater(() -> {
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
                applyMenuColors(window, tokens);
            }
        });
        for (Runnable listener : new ArrayList<Runnable>(listeners)) listener.run();
    }

    private boolean isDark(java.awt.Color color) {
        return color.getRed() * 299 + color.getGreen() * 587 + color.getBlue() * 114 < 128000;
    }

    private ThemeTokens swingResources(final ThemeTokens source) {
        return new ThemeTokens() {
            @Override public java.awt.Color color(ThemeColor token) {
                return new ColorUIResource(source.color(token));
            }
            @Override public java.awt.Font font(ThemeFont token) {
                return new FontUIResource(source.font(token));
            }
            @Override public int spacing(ThemeSpacing token) { return source.spacing(token); }
        };
    }

    static void applyMenuColors(Component component, ThemeTokens tokens) {
        if (component instanceof JMenuBar || component instanceof JPopupMenu || component instanceof JMenuItem) {
            component.setBackground(tokens.color(ThemeColor.SURFACE));
            component.setForeground(component.isEnabled()
                    ? tokens.color(ThemeColor.TEXT) : tokens.color(ThemeColor.MUTED_TEXT));
        }
        if (component instanceof JMenu) {
            JMenu menu = (JMenu) component;
            applyMenuColors(menu.getPopupMenu(), tokens);
            for (Component child : menu.getMenuComponents()) applyMenuColors(child, tokens);
            return;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) applyMenuColors(child, tokens);
        }
    }
}
