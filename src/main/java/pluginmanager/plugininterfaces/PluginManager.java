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
package pluginmanager.plugininterfaces;

import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import pluginmanager.packaging.PluginPackageInspection;
import platform.api.plugin.PluginDiagnostic;
import platform.api.plugin.PluginHealth;
import platform.api.plugin.PluginId;
import platform.api.plugin.PluginState;
import platform.api.service.ServiceKey;

public interface PluginManager {

	public void init() throws FileNotFoundException;
	public void shutdown();
	public PluginState getPluginState(PluginId id);
	public List<PluginDiagnostic> getPluginDiagnostics();
	public List<PluginHealth> getPluginHealth();
	public void setPluginActive(String pluginName, boolean active);
	public PluginPackageInspection installTrustedLocal(Path packageFile) throws IOException;
	public void uninstallPlugin(String pluginName) throws IOException;

	// service management
	
	public void registerService(String name, Service service);
	public Service getService(String string);
	public <T extends platform.api.service.Service> void registerService(ServiceKey<T> key, T service);
	public <T extends platform.api.service.Service> Optional<T> getService(ServiceKey<T> key);

	// action management
	
	public void addActionProcessor(String actionName, ActionProcessor processor);
	public void addActionProcessor(String actionName, ActionProcessor processor, int priority);
	public void removeActionProcessor(String actionName, ActionProcessor processor);
	public void callAction(String actionName, Map<String, Object> context);	
}
