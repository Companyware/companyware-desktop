package platform.api.ui;

import platform.api.service.Service;

/**
 * Stable boundary through which the platform can request authentication.
 */
public interface LoginPrompt extends Service {

	void show();
}
