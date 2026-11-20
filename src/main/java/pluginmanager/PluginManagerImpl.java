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
package pluginmanager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import core.HibernateUtils;
import models.plugin.PluginModel;
import models.plugin.Repository;
import models.pluginsettings.PluginSettingsModel;
import pluginmanager.plugininterfaces.ActionProcessor;
import pluginmanager.plugininterfaces.Plugin;
import pluginmanager.plugininterfaces.PluginManager;
import pluginmanager.plugininterfaces.Service;
import platform.api.service.ServiceKey;
import platform.api.plugin.PluginDescriptor;
import platform.api.plugin.PluginDiagnostic;
import platform.api.plugin.PluginHealth;
import pluginmanager.lifecycle.PluginDefinition;
import pluginmanager.lifecycle.PluginLifecycleEngine;
import pluginmanager.lifecycle.PluginRuntime;
import pluginmanager.packaging.PluginPackageInspection;
import pluginmanager.packaging.PluginPackageInspector;
import pluginmanager.packaging.PluginPackageInstaller;
import desktop.ui.DefaultThemeRegistry;
import desktop.ui.DefaultUiRegistry;
import platform.api.ui.PlatformServiceIds;
import platform.api.security.SecurityServiceIds;
import core.security.BCryptPasswordHasher;
import core.security.DefaultIdentityService;
import core.security.EnvironmentSecretProvider;
import core.security.HibernateUserAccountStore;
import core.security.LoggingAuditSink;
import core.security.RecoveryKeyService;
import core.security.SmtpRecoveryMailSender;
import core.i18n.DefaultLocalizationService;
import platform.api.i18n.LocalizationServiceIds;
import persistence.DefaultPluginDatabase;
import platform.api.persistence.PersistenceServiceIds;

public class PluginManagerImpl implements PluginManager {

	private String pluginsDir;
	private PluginClassLoader classLoader;
	private List<String> loadedPlugins = new ArrayList<>();
	private Map<String, Service> services = new HashMap<>();
	private Map<ServiceKey<?>, platform.api.service.Service> typedServices = new HashMap<>();
	private Boolean isJar = false;
	private PluginLifecycleEngine lifecycle = new PluginLifecycleEngine();
	private final Set<platform.api.plugin.PluginId> activationChanges = new HashSet<platform.api.plugin.PluginId>();
	private final Map<String, platform.api.plugin.PluginId> pluginIdsByName = new HashMap<String, platform.api.plugin.PluginId>();
	private final Map<platform.api.plugin.PluginId, Path> packageDirectories = new HashMap<platform.api.plugin.PluginId, Path>();
	private boolean shutdown;
	private static final Log log = LogFactory.getLog(PluginManagerImpl.class);

	public PluginManagerImpl(String pluginsDir) {
		this.pluginsDir = pluginsDir;
		this.classLoader = new PluginClassLoader();
		registerService(PlatformServiceIds.UI_REGISTRY, new DefaultUiRegistry());
		registerService(PlatformServiceIds.THEME_REGISTRY, new DefaultThemeRegistry());
		registerService(LocalizationServiceIds.LOCALIZATION, new DefaultLocalizationService());
		registerService(PersistenceServiceIds.PLUGIN_DATABASE, new DefaultPluginDatabase());
		LoggingAuditSink audit = new LoggingAuditSink();
		DefaultIdentityService identity = new DefaultIdentityService(
				new HibernateUserAccountStore(), new BCryptPasswordHasher(), audit);
		registerService(SecurityServiceIds.AUDIT, audit);
		registerService(SecurityServiceIds.AUTHENTICATION, identity);
		registerService(SecurityServiceIds.AUTHORIZATION, identity);
		EnvironmentSecretProvider secrets = new EnvironmentSecretProvider();
		registerService(SecurityServiceIds.SECRETS, secrets);
		registerService(SecurityServiceIds.MUTABLE_SECRETS, secrets);
		registerService(SecurityServiceIds.RECOVERY_KEYS,
				new RecoveryKeyService(new BCryptPasswordHasher(), audit, new SmtpRecoveryMailSender(secrets)));
	}

	@Override
	public void init() {
		URL url = getClass().getResource("/");

        URI uri = null;
		
		try {
			if(url!=null){
				uri = url.toURI();
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        Path path;
		if (uri != null && uri.getScheme().equals("jar")) {
			this.isJar = true;
        	this.pluginsDir = "classes/plugins";
			try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())) {
				path = fileSystem.getPath("/BOOT-INF/classes/plugins/");
				try (Stream<Path> walk = Files.walk(path, 1)) {
					for (Iterator<Path> it = walk.iterator(); it.hasNext();) {
						String filename = it.next().toString();
						if (filename.contains("/core/") || filename.contains("community")) {
							final boolean core = filename.contains("/core/");
							Path pathPlugins = fileSystem.getPath(filename);
							try (Stream<Path> pluginPaths = Files.walk(pathPlugins, 1)) {
								pluginPaths.forEach(pathP -> {
									File file = new File(pathP.toString());
									if (!file.getName().startsWith(".") && !file.getName().endsWith("core")
											&& !file.getName().endsWith("community")) initPlugin(file, core);
								});
							}
						}
					}
				}
			} catch (IOException failure) {
				throw new IllegalStateException("Unable to scan packaged plugins", failure);
			}
        }
        else{
        	File dh = new File(this.pluginsDir);
        
            if(dh != null){ 
			String[] directoryNames = dh.list();
			List<String> listFiles = directoryNames == null
					? Collections.<String>emptyList() : Arrays.asList(directoryNames);
    		
    			Collections.sort(listFiles,Collections.reverseOrder());
    			for (String pluginDir : listFiles){
    				if(pluginDir.equals("community") || pluginDir.equals("core")){
    					boolean core = pluginDir.equals("core")?true:false;
    					File pluginDirectory = new File(this.pluginsDir + System.getProperty("file.separator") + pluginDir); 
    					for (File plugin : pluginDirectory.listFiles()){
    						if(!plugin.getName().startsWith(".")){
    							initPlugin(plugin, core);
    						}
    					}
    				}
    			}
            }
        }
		discoverInstalledPackages();
		core.Companyware.getContainer().checkFirstRun();
		core.Companyware.getContainer().checkUpdate();
		loadPlugins();
	}
	
	@SuppressWarnings("unchecked")
	private void initPlugin(File pluginDir, boolean core) {
		try {
			String subdirectory = core?"core":"community";
			String pattern = Pattern.quote(System.getProperty("file.separator"));
			String[] pathParts = pluginDir.getName().split(pattern);
			String pluginpath = "plugins."+subdirectory+"."+pathParts[0]+"."+pathParts[0].substring(0, 1).toUpperCase() + pathParts[0].substring(1);
 	
 			
			String pluginNamespace = pathParts[0].substring(0, 1).toUpperCase() + pathParts[0].substring(1);
			File file = new File(pluginDir, pluginNamespace+".yaml");
			platform.api.plugin.PluginDescriptor descriptor = PluginDescriptorReader.read(file);
			String name = "";
			String version = "";
			String author = "";
			Integer prio = descriptor.getPriority();
			name = pluginNamespace;
			if(!name.equals("Community") && !name.equals("Core")){
				name = descriptor.getName();
				version = descriptor.getVersion().toString();
				author = descriptor.getAuthor();
			
				PluginModel pluginModel;
				try (Session session = HibernateUtils.getSessionFactory().openSession()) {
					Query query = session.createQuery("from PluginModel where name=:name");
					query.setParameter("name", name);
					pluginModel = (PluginModel) query.uniqueResult();
				}
				Long pluginId;
			    if(pluginModel == null){
			       	PluginModel newPlugin = new PluginModel();
					newPlugin.setName(name);
					newPlugin.setVersion(version);
					newPlugin.setAuthor(author);
			       	newPlugin.setActive(core);
			       	newPlugin.setNamespace(subdirectory);
			       	newPlugin.setPath(pluginpath);
			       	if(prio != null){
			       		newPlugin.setPrio(prio);
			       	}
			       	Repository.save(newPlugin);
			       	pluginId = newPlugin.getId();
			    }
			    else{
					pluginModel.setVersion(version);
					pluginModel.setAuthor(author);
					pluginModel.setPrio(prio);
					Repository.save(pluginModel);
					pluginId = pluginModel.getId();
			    }
			    this.readAndSaveXmlSettings(name, pluginId);
			}
		} catch (Exception ex) {
			log.error("Exception in PluginManagerImp loadPlugins",ex);
		}
	}
	
	private void loadPlugins() {
		try {
			List<PluginModel> plugins;
			try (Session session = HibernateUtils.getSessionFactory().openSession()) {
				Query query = session.createQuery("from PluginModel");
				plugins = query.list();
			}
			List<PluginDefinition> definitions = new ArrayList<PluginDefinition>();
			Set<platform.api.plugin.PluginId> active = new HashSet<platform.api.plugin.PluginId>();
			for (final PluginModel model : plugins) {
				try {
					final PluginDescriptor descriptor = readInstalledDescriptor(model);
					pluginIdsByName.put(model.getName(), descriptor.getId());
					if (Boolean.TRUE.equals(model.getActive())) active.add(descriptor.getId());
					definitions.add(definition(model, descriptor));
				} catch (Exception descriptorFailure) {
					log.error("Plugin descriptor rejected for " + model.getName(), descriptorFailure);
				}
			}
			lifecycle.discover(definitions);
			lifecycle.resolve();
			lifecycle.startResolved(active);
			for (PluginDiagnostic diagnostic : lifecycle.diagnostics()) {
				log.error(diagnostic.getPluginId() + ": " + diagnostic.getMessage());
			}
		} catch (Exception ex) {
			log.error("Exception in PluginManagerImp loadPlugins",ex);
		}
	}

	@Override
	public synchronized PluginPackageInspection installTrustedLocal(Path packageFile) throws IOException {
		if (shutdown) throw new IllegalStateException("Plugin manager is shut down");
		PluginPackageInspection candidate = new PluginPackageInspector().inspect(packageFile);
		final PluginDescriptor descriptor = candidate.getDescriptor();
		final platform.api.plugin.PluginId id = descriptor.getId();
		final PluginDescriptor previousDescriptor = lifecycle.descriptorOf(id);
		final boolean update = previousDescriptor != null;
		if (update) {
			if (!packageDirectories.containsKey(id))
				throw new IllegalArgumentException("Built-in plugins cannot be replaced by a package: " + id);
			if (!previousDescriptor.getName().equals(descriptor.getName()))
				throw new IllegalArgumentException("A plugin update cannot change its name");
			if (descriptor.getVersion().compareTo(previousDescriptor.getVersion()) <= 0)
				throw new IllegalArgumentException("Plugin update version must be newer than " + previousDescriptor.getVersion());
			if (lifecycle.stateOf(id) == platform.api.plugin.PluginState.STARTED)
				throw new IllegalStateException("Deactivate the plugin before updating it: " + descriptor.getName());
		} else if (pluginIdsByName.containsKey(descriptor.getName())) {
			throw new IllegalArgumentException("Plugin name is already known: " + descriptor.getName());
		}
		Path root = installedPackageRoot();
		final PluginModel model = update ? Repository.getPluginByName(previousDescriptor.getName()) : new PluginModel();
		final PluginDefinition previousDefinition = update
				? new PluginDefinition(previousDescriptor, () -> managedRuntime(model, previousDescriptor)) : null;
		final String oldVersion = update ? model.getVersion() : null;
		final String oldAuthor = update ? model.getAuthor() : null;
		final Integer oldPriority = update ? model.getPrio() : null;
		return new PluginPackageInstaller().installTrustedLocal(packageFile, root,
				new PluginPackageInstaller.InstallationCommit() {
			private boolean committed;
			@Override public void commit(PluginPackageInspection installed, Path directory) {
				if (update) updatePackageDefinition(model, previousDefinition, descriptor);
				else addPackageDefinition(model, descriptor);
				packageDirectories.put(id, directory.toAbsolutePath().normalize());
				pluginIdsByName.put(descriptor.getName(), id);
				committed = true;
			}
			@Override public void rollback() {
				if (!committed) return;
				if (update) {
					model.setVersion(oldVersion); model.setAuthor(oldAuthor); model.setPrio(oldPriority);
					lifecycle.replaceDefinition(previousDefinition); lifecycle.resolve(); Repository.save(model);
				} else {
					try { lifecycle.removeDefinition(id); Repository.deleteByName(descriptor.getName()); }
					finally { packageDirectories.remove(id); pluginIdsByName.remove(descriptor.getName()); }
				}
			}
		});
	}

	private void addPackageDefinition(PluginModel model, PluginDescriptor descriptor) {
		applyDescriptor(model, descriptor);
		lifecycle.discoverAdditional(Collections.singletonList(definition(model, descriptor)));
		try {
			lifecycle.resolve();
			if (lifecycle.stateOf(descriptor.getId()) != platform.api.plugin.PluginState.RESOLVED)
				throw new IllegalArgumentException("Plugin dependencies cannot be resolved: " + descriptor.getId());
			Repository.save(model);
		} catch (RuntimeException failure) {
			try { lifecycle.removeDefinition(descriptor.getId()); }
			catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
			throw failure;
		}
	}

	private void updatePackageDefinition(PluginModel model, PluginDefinition previous, PluginDescriptor descriptor) {
		String oldVersion = model.getVersion();
		String oldAuthor = model.getAuthor();
		Integer oldPriority = model.getPrio();
		applyDescriptor(model, descriptor);
		lifecycle.replaceDefinition(definition(model, descriptor));
		try {
			lifecycle.resolve();
			if (lifecycle.stateOf(descriptor.getId()) != platform.api.plugin.PluginState.RESOLVED)
				throw new IllegalArgumentException("Updated plugin dependencies cannot be resolved: " + descriptor.getId());
			Repository.save(model);
		} catch (RuntimeException failure) {
			model.setVersion(oldVersion); model.setAuthor(oldAuthor); model.setPrio(oldPriority);
			try { lifecycle.replaceDefinition(previous); lifecycle.resolve(); Repository.save(model); }
			catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
			throw failure;
		}
	}

	@Override
	public synchronized void uninstallPlugin(String pluginName) throws IOException {
		if (shutdown) throw new IllegalStateException("Plugin manager is shut down");
		final platform.api.plugin.PluginId id = pluginIdsByName.get(pluginName);
		if (id == null) throw new IllegalArgumentException("Unknown plugin: " + pluginName);
		final Path directory = packageDirectories.get(id);
		if (directory == null) throw new IllegalArgumentException("Built-in plugins cannot be uninstalled: " + pluginName);
		if (lifecycle.stateOf(id) == platform.api.plugin.PluginState.STARTED)
			throw new IllegalStateException("Deactivate the plugin before uninstalling it: " + pluginName);
		final PluginModel model = Repository.getPluginByName(pluginName);
		new PluginPackageInstaller().removeInstalled(installedPackageRoot(), id.value(),
				new PluginPackageInstaller.RemovalCommit() {
			private PluginDefinition removedDefinition;
			@Override public void commit(Path removed) {
				removedDefinition = lifecycle.removeDefinition(id);
				Repository.deleteByName(pluginName);
				packageDirectories.remove(id);
				pluginIdsByName.remove(pluginName);
			}
			@Override public void rollback() {
				if (removedDefinition != null && lifecycle.descriptorOf(id) == null) {
					lifecycle.discoverAdditional(Collections.singletonList(removedDefinition));
					lifecycle.resolve();
				}
				if (Repository.getPluginByName(pluginName) == null) Repository.save(model);
				packageDirectories.put(id, directory);
				pluginIdsByName.put(pluginName, id);
			}
		});
	}

	private void discoverInstalledPackages() {
		Path root = installedPackageRoot();
		if (!Files.isDirectory(root)) return;
		try (Stream<Path> paths = Files.list(root)) {
			List<Path> directories = new ArrayList<Path>();
			paths.filter(Files::isDirectory).filter(path -> !path.getFileName().toString().startsWith("."))
					.forEach(directories::add);
			Collections.sort(directories);
			for (Path directory : directories) {
				try {
					PluginDescriptor descriptor = validateInstalledPackage(directory);
					registerPackageModel(descriptor);
					packageDirectories.put(descriptor.getId(), directory.toAbsolutePath().normalize());
				} catch (Exception failure) {
					log.error("Installed plugin package rejected: " + directory, failure);
				}
			}
		} catch (IOException failure) {
			throw new IllegalStateException("Unable to scan installed plugin packages", failure);
		}
	}

	private PluginDescriptor validateInstalledPackage(Path directory) throws IOException {
		Path descriptorFile = directory.resolve(PluginPackageInspector.DESCRIPTOR);
		Path jar = directory.resolve(PluginPackageInspector.PLUGIN_JAR);
		Path metadataFile = directory.resolve(".companyware-package.properties");
		if (!Files.isRegularFile(descriptorFile) || !Files.isRegularFile(jar) || !Files.isRegularFile(metadataFile))
			throw new IllegalArgumentException("Installed package is incomplete");
		Properties metadata = new Properties();
		try (InputStream input = Files.newInputStream(metadataFile)) { metadata.load(input); }
		if (!"trusted-local".equals(metadata.getProperty("trust")))
			throw new IllegalArgumentException("Installed package is not trusted-local");
		PluginDescriptor descriptor = PluginDescriptorReader.read(descriptorFile.toFile());
		if (!descriptor.getId().value().equals(metadata.getProperty("id"))
				|| !descriptor.getId().value().equals(directory.getFileName().toString()))
			throw new IllegalArgumentException("Installed package identity does not match metadata");
		return descriptor;
	}

	private PluginModel registerPackageModel(PluginDescriptor descriptor) {
		PluginModel model = Repository.getPluginByName(descriptor.getName());
		if (model != null && (model.getPath() == null
				|| !("external:" + descriptor.getId().value()).equals(model.getPath())))
			throw new IllegalArgumentException("Plugin name is already installed: " + descriptor.getName());
		if (model == null) {
			model = new PluginModel();
			model.setName(descriptor.getName());
			model.setActive(false);
		}
		applyDescriptor(model, descriptor);
		Repository.save(model);
		return model;
	}

	private void applyDescriptor(PluginModel model, PluginDescriptor descriptor) {
		model.setName(descriptor.getName());
		if (model.getActive() == null) model.setActive(false);
		model.setVersion(descriptor.getVersion().toString());
		model.setAuthor(descriptor.getAuthor());
		model.setPrio(descriptor.getPriority());
		model.setNamespace("external");
		model.setPath("external:" + descriptor.getId().value());
	}

	private Path installedPackageRoot() {
		return Paths.get(System.getProperty("companyware.plugins.directory", "plugins-installed"));
	}

	@Override public synchronized void setPluginActive(String pluginName, boolean active) {
		PluginModel model = Repository.getPluginByName(pluginName);
		platform.api.plugin.PluginId id = pluginIdsByName.get(pluginName);
		if (id == null) throw new IllegalArgumentException("Unknown plugin: " + pluginName);
		boolean previous = Boolean.TRUE.equals(model.getActive());
		if (previous == active) return;
		if (!activationChanges.add(id)) return;
		try {
			if (active) lifecycle.activate(id); else lifecycle.deactivate(id);
			model.setActive(active);
			Repository.save(model);
		} catch (RuntimeException persistenceFailure) {
			try { if (active) lifecycle.deactivate(id); else lifecycle.activate(id); }
			catch (RuntimeException rollbackFailure) { persistenceFailure.addSuppressed(rollbackFailure); }
			throw persistenceFailure;
		} finally {
			activationChanges.remove(id);
		}
	}

	private PluginDescriptor readInstalledDescriptor(PluginModel model) throws IOException {
		if (model.getPath().startsWith("external:")) {
			platform.api.plugin.PluginId id = platform.api.plugin.PluginId.of(model.getPath().substring("external:".length()));
			Path directory = packageDirectories.get(id);
			if (directory == null) throw new IOException("Installed package directory is unavailable: " + id);
			return validateInstalledPackage(directory);
		}
		String entryPoint = model.getPath();
		String relative = entryPoint.substring("plugins.".length()).replace('.', File.separatorChar) + ".yaml";
		return PluginDescriptorReader.read(new File(pluginsDir, relative));
	}

	private PluginDefinition definition(final PluginModel model, final PluginDescriptor descriptor) {
		return new PluginDefinition(descriptor, () -> managedRuntime(model, descriptor));
	}

	private PluginRuntime managedRuntime(final PluginModel model, final PluginDescriptor descriptor) {
		return new PluginRuntime() {
			private Object instance;
			private DefaultPluginContext context;
			private Map<String, Service> previousServices;
			private Map<ServiceKey<?>, platform.api.service.Service> previousTyped;
			private Map<String, ActionProcessorList> previousActions;
			private PackagePluginClassLoader packageLoader;
			@Override public void start() throws Exception {
				previousServices = new HashMap<String, Service>(services);
				previousTyped = new HashMap<ServiceKey<?>, platform.api.service.Service>(typedServices);
				previousActions = copyActionProcessors();
				Path packageDirectory = packageDirectories.get(descriptor.getId());
				ClassLoader runtimeLoader = classLoader;
				if (packageDirectory != null) {
					packageLoader = new PackagePluginClassLoader(packageDirectory.resolve(PluginPackageInspector.PLUGIN_JAR),
							PluginManagerImpl.class.getClassLoader());
					runtimeLoader = packageLoader;
				}
				Class<?> pluginClass = runtimeLoader.loadClass(descriptor.getEntryPoint());
				instance = pluginClass.newInstance();
				if (instance instanceof Plugin) {
					((Plugin) instance).init(PluginManagerImpl.this);
				} else if (instance instanceof platform.api.plugin.Plugin) {
					context = new DefaultPluginContext(descriptor, PluginManagerImpl.this);
					((platform.api.plugin.Plugin) instance).initialize(context);
				} else {
					throw new IllegalArgumentException("Entry point must implement the legacy or SDK Plugin contract: " + descriptor.getEntryPoint());
				}
				loadedPlugins.add(model.getPath());
			}
			@Override public void stop() throws Exception {
				Exception cleanupFailure = null;
				if (instance instanceof platform.api.plugin.Plugin) {
					try { ((platform.api.plugin.Plugin) instance).stop(); }
					catch (Exception exception) { cleanupFailure = exception; }
				}
				if (context != null) {
					try { context.closeResources(); }
					catch (Exception exception) { if (cleanupFailure == null) cleanupFailure = exception; }
				}
				if (context == null) {
					services.clear(); services.putAll(previousServices);
					typedServices.clear(); typedServices.putAll(previousTyped);
					actionProcessors.clear(); actionProcessors.putAll(previousActions);
				}
				loadedPlugins.remove(model.getPath());
				if (packageLoader != null) {
					try { packageLoader.close(); }
					catch (Exception exception) {
						if (cleanupFailure == null) cleanupFailure = exception;
						else cleanupFailure.addSuppressed(exception);
					} finally { packageLoader = null; }
				}
				if (cleanupFailure != null) throw cleanupFailure;
			}
		};
	}

	private Map<String, ActionProcessorList> copyActionProcessors() {
		Map<String, ActionProcessorList> copy = new HashMap<String, ActionProcessorList>();
		for (Map.Entry<String, ActionProcessorList> entry : actionProcessors.entrySet()) {
			copy.put(entry.getKey(), entry.getValue().copy());
		}
		return copy;
	}

	@Override
	public synchronized void shutdown() {
		if (shutdown) return;
		shutdown = true;
		lifecycle.stopAll();
		try {
			classLoader.close();
		} catch (IOException exception) {
			log.error("Unable to close plugin classloader", exception);
		}
	}

	@Override
	public platform.api.plugin.PluginState getPluginState(platform.api.plugin.PluginId id) {
		return lifecycle.stateOf(id);
	}

	@Override
	public List<PluginDiagnostic> getPluginDiagnostics() {
		return lifecycle.diagnostics();
	}

	@Override
	public List<PluginHealth> getPluginHealth() {
		return lifecycle.health();
	}
	
	public void readAndSaveXmlSettings(String pluginName, Long pluginId) throws ParserConfigurationException, SAXException, IOException{
	    //Get Document Builder
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    
	    String fileSeparator = FileSystems.getDefault().getSeparator();
	    
	    File file;
	    if(this.isJar){
	    	file = new File("classes"+fileSeparator+"plugins"+fileSeparator+"community/"+pluginName.substring(0, 1).toLowerCase()+pluginName.substring(1)+fileSeparator+pluginName+".xml");
	    }
	    else{
	    	file = new File("target"+fileSeparator+"classes"+fileSeparator+"plugins"+fileSeparator+"community/"+pluginName.substring(0, 1).toLowerCase()+pluginName.substring(1)+fileSeparator+pluginName+".xml");
	    }
	    
	    if(file.exists()){
	    	
	    	//Build Document
	    	Document document = builder.parse(file);

	    	//Normalize the XML Structure; It's just too important !!
	    	document.getDocumentElement().normalize();

	    	//Here comes the root node
	    	Element root = document.getDocumentElement();

	    	//Get all settings
	    	NodeList nList = document.getElementsByTagName("Setting");
	  
	    	for (int temp = 0; temp < nList.getLength(); temp++)
	    	{
	    		Node node = nList.item(temp);
	    		if (node.getNodeType() == Node.ELEMENT_NODE)
	    		{
	    			Element eElement = (Element) node;
	    			String settingType = eElement.getElementsByTagName("type").item(0).getTextContent();
	    			String settingName = eElement.getElementsByTagName("name").item(0).getTextContent();
	    			String settingValue = eElement.getElementsByTagName("value").item(0).getTextContent();
	    		
				List<Object[]> settings;
				try (Session session = HibernateUtils.getSessionFactory().openSession()) {
					Query query= session.createQuery(""
	    			        		+ "from PluginModel pm "
	    			        		+ "join PluginSettingsModel sm "
	    			        		+ "on pm.id = sm.pluginId "
	    			        		+ "where pm.name=:name "
	    			        		+ "and sm.name=:settingName "
	    			        		+ "");
					query.setParameter("name", pluginName);
					query.setParameter("settingName", settingName);
					settings = query.list();
				}
	    		
	    		    if(settings.isEmpty()){   
	    		       PluginSettingsModel newSetting = new PluginSettingsModel();
	   		       	   newSetting.setPluginId(pluginId);
	   		       	   newSetting.setName(settingName);
	   		       	   newSetting.setType(settingType);
	   		       	   newSetting.setValue(settingValue);
	   		       	   models.pluginsettings.Repository.save(newSetting);
	    		    }
	    		
	    		}
	    	}
	    }
	}

	@Override
	public void registerService(String name, Service service) {
		services.put(name, service);
	}

	@Override
	public Service getService(String name) {
		return services.get(name);
	}

	@Override
	public <T extends platform.api.service.Service> void registerService(ServiceKey<T> key, T service) {
		if (typedServices.containsKey(key)) {
			throw new IllegalStateException("Service already registered: " + key);
		}
		typedServices.put(key, key.getServiceType().cast(service));
	}

	@Override
	public <T extends platform.api.service.Service> Optional<T> getService(ServiceKey<T> key) {
		platform.api.service.Service service = typedServices.get(key);
		return service == null ? Optional.<T>empty() : Optional.of(key.getServiceType().cast(service));
	}

	<T extends platform.api.service.Service> void unregisterService(ServiceKey<T> key, T service) {
		if (typedServices.get(key) == service) typedServices.remove(key);
	}

	private class ActionProcessorElement implements Comparable<ActionProcessorElement> {

		private ActionProcessor processor;
		private int priority;
				
		public ActionProcessorElement(ActionProcessor processor, int priority) {
			this.processor = processor;
			this.priority = priority;
		}

		@Override
		public int compareTo(ActionProcessorElement o) {
			return o.priority - this.priority;
		}

		public ActionProcessor getProcessor() {
			return processor;
		}
	}

	private class ActionProcessorList {
		
		private List<ActionProcessorElement> actionProcessorElements = new ArrayList<ActionProcessorElement>();

		public ActionProcessorList copy() {
			ActionProcessorList copy = new ActionProcessorList();
			copy.actionProcessorElements.addAll(actionProcessorElements);
			return copy;
		}

		public void addActionProcessor(ActionProcessor processor, int priority) {
			actionProcessorElements.add(new ActionProcessorElement(processor, priority));
			Collections.sort(actionProcessorElements);
		}

		public void removeActionProcessor(ActionProcessor processor) {
			ActionProcessorElement element = null;
			for(ActionProcessorElement ape : actionProcessorElements) {
				if(ape.getProcessor() == processor)
					element = ape;
			}
			if(element != null)
				actionProcessorElements.remove(element);
		}

		public void call(Map<String, Object> context) {
			for(ActionProcessorElement ape : actionProcessorElements) {
				ape.getProcessor().call(context);
			}
		}	
	}
	
	private Map<String, ActionProcessorList> actionProcessors = new HashMap<String, ActionProcessorList>(); 
		
	@Override
	public void addActionProcessor(String actionName, ActionProcessor processor) {
		addActionProcessor(actionName, processor, 0);
	}

	@Override
	public void addActionProcessor(String actionName,
			ActionProcessor processor, int priority) {
		ActionProcessorList apl = actionProcessors.get(actionName);
		if(apl == null)
			actionProcessors.put(actionName, apl = new ActionProcessorList());
		apl.addActionProcessor(processor, priority);
	}

	@Override
	public void removeActionProcessor(String actionName,
			ActionProcessor processor) {
		ActionProcessorList apl = actionProcessors.get(actionName);
		if(apl == null)
			return;
		apl.removeActionProcessor(processor);
	}

	@Override
	public void callAction(String actionName, Map<String, Object> context) {
		ActionProcessorList apl = actionProcessors.get(actionName);
		if(apl == null)
			return;
		apl.call(context);
	}
}
