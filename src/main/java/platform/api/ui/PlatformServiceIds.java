package platform.api.ui;

/**
 * Service identifiers shared by the platform and its built-in UI plugins.
 * Typed registration replaces these identifiers in CW-D-002.
 */
public final class PlatformServiceIds {

	public static final platform.api.service.ServiceKey<DesktopShell> DESKTOP_SHELL =
			platform.api.service.ServiceKey.of("platform.desktop-shell", DesktopShell.class);
	public static final platform.api.service.ServiceKey<LoginPrompt> LOGIN_PROMPT =
			platform.api.service.ServiceKey.of("platform.login-prompt", LoginPrompt.class);
	public static final platform.api.service.ServiceKey<UiRegistry> UI_REGISTRY =
			platform.api.service.ServiceKey.of("platform.ui-registry", UiRegistry.class);
	public static final platform.api.service.ServiceKey<platform.api.theme.ThemeRegistry> THEME_REGISTRY =
			platform.api.service.ServiceKey.of("platform.theme-registry", platform.api.theme.ThemeRegistry.class);

	private PlatformServiceIds() {
	}
}
