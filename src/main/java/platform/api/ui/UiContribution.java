package platform.api.ui;

import javax.swing.JComponent;
import platform.api.plugin.PluginId;

/** Base contract for Swing contributions owned by a plugin. */
public interface UiContribution {
    PluginId owner();
    String id();
    int order();
    JComponent component();
}
