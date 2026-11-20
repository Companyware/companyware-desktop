/*******************************************************************************
 * MIT License
 *
 * Companyware - a java desktop framework for plugins
 *
 * Copyright (c) 2023 mbdus-Softwareentwicklung - Mathias Bauer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package plugins.core.frame;

import plugins.core.frame.controller.FrameController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import pluginmanager.plugininterfaces.Plugin;
import pluginmanager.plugininterfaces.PluginManager;
import platform.api.ui.PlatformServiceIds;
import platform.api.ui.UiRegistry;
import platform.api.theme.ThemeRegistry;

public class Frame implements Plugin {
	
	private static final Log log = LogFactory.getLog(Frame.class);

	@Override
	public void init(PluginManager pm) {
		UiRegistry registry = pm.getService(PlatformServiceIds.UI_REGISTRY)
				.orElseThrow(() -> new IllegalStateException("UI registry is unavailable"));
		FrameController controller = new FrameController(registry);
		ThemeRegistry themes = pm.getService(PlatformServiceIds.THEME_REGISTRY)
				.orElseThrow(() -> new IllegalStateException("Theme registry is unavailable"));
		themes.onChange(controller::refreshTheme);
		pm.registerService(PlatformServiceIds.DESKTOP_SHELL, controller);
		pm.registerService("FrameController", controller);
	}
}
