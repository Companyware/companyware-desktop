package architecture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Guards the CW-D-001 rule that platform implementation code must not import
 * concrete plugins. This source-level test also works before the legacy code is
 * split into physical Maven modules.
 */
public class ArchitectureBoundariesTest {

	@Test
	public void platformDoesNotImportConcretePlugins() throws IOException {
		assertNoPluginImports(Paths.get("src/main/java/Companyware.java"));
		assertNoPluginImportsBelow(Paths.get("src/main/java/core"));
		assertNoPluginImportsBelow(Paths.get("src/main/java/pluginmanager"));
	}

	@Test
	public void publicApiDoesNotImportImplementations() throws IOException {
		Path apiRoot = Paths.get("src/main/java/platform/api");
		try (Stream<Path> paths = Files.walk(apiRoot)) {
			for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
				String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
				assertFalse(path + " imports platform implementation code",
						source.matches("(?s).*\\bimport\\s+(core|models|plugins|pluginmanager)\\..*"));
			}
		}
	}

	@Test
	public void sdkExampleImportsOnlyPublicApi() throws IOException {
		Path example = Paths.get("src/test/java/sdkexample/ExamplePlugin.java");
		String source = new String(Files.readAllBytes(example), StandardCharsets.UTF_8);
		assertFalse("SDK example imports implementation code",
				source.matches("(?s).*\\bimport\\s+(core|models|plugins|pluginmanager)\\..*"));
	}

	@Test
	public void referencePluginsImportOnlyTheirOwnCodeAndPublicApi() throws IOException {
		assertSdkPlugin(Paths.get("src/main/java/plugins/community/companywaredemo"));
		assertSdkPlugin(Paths.get("src/main/java/plugins/community/mbduscustomer"));
		assertSdkPlugin(Paths.get("src/main/java/plugins/community/helloworld"));
		assertSdkPlugin(Paths.get("src/main/java/plugins/community/darktheme"));
		assertSdkPlugin(Paths.get("src/main/java/plugins/community/invoice"));
	}

	@Test
	public void businessReferencePluginsContributeMenuAndNavigation() throws IOException {
		assertMenuAndNavigation(Paths.get(
				"src/main/java/plugins/community/companywaredemo/Companywaredemo.java"));
		assertMenuAndNavigation(Paths.get(
				"src/main/java/plugins/community/mbduscustomer/Mbduscustomer.java"));
		assertMenuAndNavigation(Paths.get(
				"src/main/java/plugins/community/invoice/Invoice.java"));
	}

	@Test
	public void businessPresentationUsesApplicationPortsInsteadOfPersistence() throws IOException {
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/companywaredemo/view"),
				"java.sql", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/mbduscustomer/view"),
				"java.sql", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/helloworld/view"),
				"java.sql", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/invoice/domain"),
				"java.sql", "javax.swing", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/invoice/view"),
				"java.sql", "platform.api.persistence", "plugins.community.invoice.infrastructure");
	}

	@Test
	public void businessApplicationPortsAreTransportAndUiIndependent() throws IOException {
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/companywaredemo/application"),
				"java.sql", "javax.swing", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/mbduscustomer/application"),
				"java.sql", "javax.swing", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/helloworld/application"),
				"java.sql", "javax.swing", "platform.api.persistence");
		assertLayerHasNoImports(Paths.get("src/main/java/plugins/community/invoice/application"),
				"java.sql", "javax.swing", "platform.api.persistence");
	}

	@Test
	public void swingExitHandlersDoNotBlockTheEventThreadWithSystemExit() throws IOException {
		assertNoDirectSystemExit(Paths.get(
				"src/main/java/plugins/core/frame/view/Frame.java"));
		assertNoDirectSystemExit(Paths.get(
				"src/main/java/plugins/core/menu/controller/MenuController.java"));
		assertNoDirectSystemExit(Paths.get(
				"src/main/java/plugins/core/login/controller/LoginController.java"));
		String bootstrap = new String(Files.readAllBytes(Paths.get("src/main/java/Companyware.java")),
				StandardCharsets.UTF_8);
		assertTrue("Spring cleanup must run before JVM shutdown",
				bootstrap.contains("ApplicationShutdown.configure(context::close)"));
		assertTrue("Spring's competing JVM shutdown hook must be disabled",
				bootstrap.contains("registerShutdownHook(false)"));
	}

	@Test
	public void addUserViewUsesTheSharedWorkspaceGridAndAGroupedForm() throws IOException {
		String source = new String(Files.readAllBytes(Paths.get(
				"src/main/java/plugins/core/users/view/Users.java")), StandardCharsets.UTF_8);
		String addUserView = source.substring(source.indexOf("public void addUserView()"),
				source.indexOf("public JTextField getNameTextField()"));
		assertTrue("Add-user heading must use the shared workspace grid",
				addUserView.contains("StandardView.create("));
		assertTrue("Add-user columns must form one centered group",
				addUserView.contains("new GridBagLayout()")
				&& addUserView.contains("new java.awt.GridLayout(1, 2, 24, 0)"));
		assertFalse("Add-user columns must not be pinned to opposite workspace edges",
				addUserView.contains("BorderLayout.WEST") || addUserView.contains("BorderLayout.EAST"));
	}

	private void assertNoDirectSystemExit(Path handler) throws IOException {
		String source = new String(Files.readAllBytes(handler), StandardCharsets.UTF_8);
		assertFalse(handler + " must delegate shutdown away from the Swing EDT",
				source.contains("System.exit("));
	}

	private void assertLayerHasNoImports(Path root, String... forbidden) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
				String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
				for (String prefix : forbidden) {
					assertFalse(path + " imports forbidden layer " + prefix,
							source.matches("(?s).*\\bimport\\s+" + Pattern.quote(prefix) + "\\..*"));
				}
			}
		}
	}

	private void assertMenuAndNavigation(Path entryPoint) throws IOException {
		String source = new String(Files.readAllBytes(entryPoint), StandardCharsets.UTF_8);
		assertTrue(entryPoint + " must contribute a menu item", source.contains("addLocalizedMenuItem("));
		assertTrue(entryPoint + " must contribute navigation", source.contains("addLocalizedNavigation("));
		assertTrue(entryPoint + " must contribute its target view", source.contains("addLocalizedView("));
	}

	private void assertSdkPlugin(Path root) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
				String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
				assertFalse(path + " imports platform implementation code",
						source.matches("(?s).*\\bimport\\s+(core|models|pluginmanager|persistence|plugins\\.core)\\..*"));
			}
		}
	}

	private void assertNoPluginImportsBelow(Path root) throws IOException {
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".java"))::iterator) {
				assertNoPluginImports(path);
			}
		}
	}

	private void assertNoPluginImports(Path path) throws IOException {
		String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
		assertFalse(path + " imports a concrete plugin", source.matches("(?s).*\\bimport\\s+plugins\\..*"));
	}
}
