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
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.annotation.PreDestroy;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import core.ApplicationContextProvider;
import core.ApplicationShutdown;
import core.HibernateUtils;
import core.StartProgressBar;
import platform.api.ui.DesktopShell;
import platform.api.ui.LoginPrompt;
import platform.api.ui.PlatformServiceIds;
import pluginmanager.PluginManagerFactory;
import pluginmanager.plugininterfaces.PluginManager;
import persistence.EmbeddedInstanceGuard;
import persistence.EmbeddedDatabaseInUseException;
import persistence.EmbeddedDatabaseProbe;
import persistence.PersistenceSettings;
import platform.api.plugin.PluginHealth;
import core.diagnostics.StartupDiagnostics;
import core.diagnostics.StructuredLog;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;


@ComponentScan(basePackages = "/src/main")
@SpringBootApplication
public class Companyware implements CommandLineRunner{

	private static final Log log = LogFactory.getLog(Companyware.class);
	private static StartProgressBar startupWindow;
	private static EmbeddedInstanceGuard instanceGuard;
	private PluginManager pluginManager;
	
	@Autowired
	private Environment env;

	public static void main(String[] args) throws Exception{
		try {
			instanceGuard = EmbeddedInstanceGuard.acquire(PersistenceSettings.load());
		} catch (EmbeddedDatabaseInUseException alreadyRunning) {
			showAlreadyRunningMessage();
			return;
		}
		showStartupWindow();
		try {
			EmbeddedDatabaseProbe.verifyAvailable(PersistenceSettings.load());
			ConfigurableApplicationContext context = new SpringApplicationBuilder(Companyware.class)
					.headless(false)
					.registerShutdownHook(false)
					.run(args);
			ApplicationShutdown.configure(context::close);
		} catch (EmbeddedDatabaseInUseException alreadyRunning) {
			closeStartupWindow();
			closeInstanceGuard();
			showAlreadyRunningMessage();
		} catch (Exception exception) {
			closeStartupWindow();
			closeInstanceGuard();
			throw exception;
		}
	}

	private static void showAlreadyRunningMessage() throws Exception {
		SwingUtilities.invokeAndWait(() -> JOptionPane.showMessageDialog(null,
				"Companyware läuft bereits mit dieser eingebetteten Datenbank.\n"
				+ "Bitte beenden Sie zuerst die andere Companyware-Instanz.\n"
				+ "Löschen Sie keine Derby-Sperrdateien.",
				"Companyware läuft bereits", JOptionPane.WARNING_MESSAGE));
	}

	private static synchronized void closeInstanceGuard() {
		if (instanceGuard == null) return;
		try { instanceGuard.close(); }
		catch (IOException failure) { log.warn("Unable to release embedded database instance lock", failure); }
		instanceGuard = null;
	}

	private static void showStartupWindow() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				startupWindow = new StartProgressBar();
				try {
					startupWindow.initializeUI();
				} catch (IOException | java.net.URISyntaxException exception) {
					log.error("Unable to display startup window", exception);
					startupWindow = null;
				}
			}
		});
	}

	private static void closeStartupWindow() {
		if (startupWindow == null) {
			return;
		}
		Runnable close = new Runnable() {
			@Override
			public void run() {
				startupWindow.finish();
				startupWindow = null;
			}
		};
		if (SwingUtilities.isEventDispatchThread()) {
			close.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(close);
			} catch (Exception exception) {
				log.error("Unable to close startup window", exception);
			}
		}
	}
	
	@Override
    public void run(String... args) throws FileNotFoundException {
		new ApplicationContextProvider();
		PluginManager pm = PluginManagerFactory.createPluginManager("target/classes/plugins");
		this.pluginManager = pm;
		pm.init();
		int failedPlugins = 0;
		for (PluginHealth health : pm.getPluginHealth()) {
			Map<String, Object> fields = new HashMap<String, Object>();
			fields.put("pluginId", health.getPluginId());
			fields.put("state", health.getState());
			fields.put("healthy", health.isHealthy());
			fields.put("message", health.getMessage());
			log.info(StructuredLog.event("plugin.health", fields));
			if (!health.isHealthy()) failedPlugins++;
		}
		log.info(StructuredLog.event("application.started",
				StartupDiagnostics.capture(PersistenceSettings.load(), pm.getPluginHealth().size(), failedPlugins)));
		DesktopShell desktopShell = pm.getService(PlatformServiceIds.DESKTOP_SHELL).orElse(null);
		LoginPrompt loginPrompt = pm.getService(PlatformServiceIds.LOGIN_PROMPT).orElse(null);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				closeStartupWindow();
				if (desktopShell != null) {
					desktopShell.show();
				}
				if (loginPrompt != null) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							loginPrompt.show();
						}
					});
				}
			}
		});
	}
	
	@PreDestroy
	public void onExit() {
		log.info("###STOP FROM THE LIFECYCLE###");
		try {
			if (pluginManager != null) pluginManager.shutdown();
		} catch (RuntimeException failure) {
			log.error("Unable to stop all plugins cleanly", failure);
		}
		try {
			HibernateUtils.shutdown();
		} catch (Exception failure) {
			log.error("Unable to close Hibernate", failure);
		} finally {
			closeInstanceGuard();
		}
	}
}
