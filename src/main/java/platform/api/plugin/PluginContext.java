package platform.api.plugin;

import java.util.Optional;
import platform.api.configuration.PluginConfiguration;
import platform.api.event.EventPublisher;
import platform.api.i18n.PluginMessages;
import platform.api.service.ServiceKey;
import platform.api.service.Service;
import platform.api.service.CapabilityRegistry;
import platform.api.ui.AccessRequirement;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import platform.api.ui.UiRegistry;
import platform.api.theme.ThemeRegistry;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.JComponent;
import platform.api.security.PermissionId;
import platform.api.persistence.PluginDatabase;
import platform.api.persistence.PersistenceServiceIds;

/** Services supplied by the platform to an SDK plugin. */
public interface PluginContext {
    PluginDescriptor descriptor();
    PluginConfiguration configuration();
    PluginMessages messages();
    EventPublisher events();
    CapabilityRegistry capabilities();
    <T extends Service> void register(ServiceKey<T> key, T service);
    <T extends Service> Optional<T> find(ServiceKey<T> key);
    UiRegistry ui();
    ThemeRegistry themes();

    default PluginDatabase database() {
        return find(PersistenceServiceIds.PLUGIN_DATABASE)
                .orElseThrow(() -> new IllegalStateException("Plugin database service is unavailable"));
    }

    /** Creates a namespace-safe permission owned by this plugin. */
    default PermissionId permission(String name) { return PermissionId.of(descriptor().getId(), name); }

    default void addView(String id, String title, int order, Supplier<? extends JComponent> component) {
        closeOnStop(ui().register(new UiContributionSpec(descriptor().getId(), id, title,
                UiContributionType.VIEW, order, null, AccessRequirement.AUTHENTICATED, component, null)));
    }

    default void addLocalizedView(String id, String titleKey, int order,
            Supplier<? extends JComponent> component) {
        closeOnStop(ui().register(UiContributionSpec.localized(descriptor().getId(), id,
                () -> messages().text(titleKey), UiContributionType.VIEW, order, null,
                AccessRequirement.AUTHENTICATED, component, null)));
    }

    default void addNavigation(String id, String title, int order, String targetView) {
		addNavigation(id, title, order, targetView, null);
	}

	default void addNavigation(String id, String title, int order, String targetView, Icon icon) {
        closeOnStop(ui().register(new UiContributionSpec(descriptor().getId(), id, title,
				UiContributionType.NAVIGATION, order, targetView, AccessRequirement.AUTHENTICATED,
				null, null, icon)));
    }

    default void addLocalizedNavigation(String id, String titleKey, int order, String targetView) {
		addLocalizedNavigation(id, titleKey, order, targetView, null);
	}

	default void addLocalizedNavigation(String id, String titleKey, int order, String targetView, Icon icon) {
        closeOnStop(ui().register(UiContributionSpec.localized(descriptor().getId(), id,
                () -> messages().text(titleKey), UiContributionType.NAVIGATION, order, targetView,
                AccessRequirement.AUTHENTICATED, null, null, icon)));
    }

    /** Adds a clickable OutlookBar group containing the following owned navigation items. */
    default void addLocalizedNavigationGroup(String id, String titleKey, int order) {
        closeOnStop(ui().register(UiContributionSpec.localized(descriptor().getId(), id,
                () -> messages().text(titleKey), UiContributionType.NAVIGATION, order, null,
                AccessRequirement.AUTHENTICATED, null, null)));
    }

    default void addMenuItem(String id, String title, int order, String menuPath, Runnable command) {
        closeOnStop(ui().register(new UiContributionSpec(descriptor().getId(), id, title,
                UiContributionType.MENU, order, menuPath, AccessRequirement.AUTHENTICATED, null, command)));
    }

	default void addLocalizedMenuItem(String id, String titleKey, int order, String menuPath, Runnable command) {
		closeOnStop(ui().register(UiContributionSpec.localized(descriptor().getId(), id,
				() -> messages().text(titleKey), UiContributionType.MENU, order, menuPath,
				AccessRequirement.AUTHENTICATED, null, command)));
	}

    default void addDialog(String id, String title, int order, Supplier<? extends JComponent> component) {
        closeOnStop(ui().register(new UiContributionSpec(descriptor().getId(), id, title,
                UiContributionType.DIALOG, order, null, AccessRequirement.AUTHENTICATED, component, null)));
    }

    default void addSettingsPage(String id, String title, int order, Supplier<? extends JComponent> component) {
        closeOnStop(ui().register(new UiContributionSpec(descriptor().getId(), id, title,
                UiContributionType.SETTINGS_PAGE, order, null, AccessRequirement.AUTHENTICATED, component, null)));
    }

    /** Registers a resource which the platform closes automatically on stop or failed startup. */
    void closeOnStop(AutoCloseable resource);
}
