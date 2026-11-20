package platform.api.ui;

import javax.swing.JComponent;
import platform.api.service.Service;

/**
 * Stable boundary through which the platform can display the desktop shell.
 */
public interface DesktopShell extends Service {

	void show();

	void showMessage(String message);

	void showView(JComponent component);
}
