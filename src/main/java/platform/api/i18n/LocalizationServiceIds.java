package platform.api.i18n;

import platform.api.service.ServiceKey;

public final class LocalizationServiceIds {
    public static final ServiceKey<LocalizationService> LOCALIZATION =
            ServiceKey.of("platform.localization", LocalizationService.class);
    private LocalizationServiceIds() { }
}
