package platform.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Optional;

import org.junit.Test;

import platform.api.plugin.PluginDescriptor;
import platform.api.service.Service;
import platform.api.service.ServiceKey;
import platform.api.version.SemanticVersion;
import pluginmanager.PluginDescriptorReader;
import pluginmanager.PluginManagerImpl;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import platform.api.plugin.PluginId;
import platform.api.ui.AccessRequirement;
import platform.api.ui.StandardView;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class PluginSdkTest {
	@Test public void standardViewUsesTheSharedWorkspaceGrid() {
		JPanel view = StandardView.create("Title", new JLabel("Content"), new JButton("Action"));
		java.awt.Insets insets = view.getBorder().getBorderInsets(view);
		assertEquals(StandardView.TOP_GAP, insets.top);
		assertEquals(StandardView.OUTER_GAP, insets.left);
		JPanel header = (JPanel) ((java.awt.BorderLayout) view.getLayout())
				.getLayoutComponent(java.awt.BorderLayout.NORTH);
		assertEquals(StandardView.HEADER_HEIGHT, header.getPreferredSize().height);
	}

    @Test
    public void semanticVersionsAreOrderedAndApiCompatible() {
        SemanticVersion required = SemanticVersion.parse("1.0.0");
        SemanticVersion current = SemanticVersion.parse("1.2.0");
        assertTrue(current.isApiCompatibleWith(required));
        assertFalse(SemanticVersion.parse("2.0.0").isApiCompatibleWith(required));
        assertTrue(current.compareTo(required) > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void malformedPluginIdIsRejected() {
        platform.api.plugin.PluginId.of("Not Valid");
    }

    @Test
    public void allBuiltInDescriptorsAreValid() throws Exception {
        File root = new File("src/main/java/plugins");
        int count = 0;
        for (File descriptor : listDescriptors(root)) {
            PluginDescriptor parsed = PluginDescriptorReader.read(descriptor);
            assertTrue(parsed.getEntryPoint().startsWith("plugins."));
            Class.forName(parsed.getEntryPoint());
            count++;
        }
        assertEquals(11, count);
    }

    @Test
    public void typedRegistryReturnsOnlyTheDeclaredType() {
        PluginManagerImpl manager = new PluginManagerImpl("unused");
        ServiceKey<TestService> key = ServiceKey.of("test.service", TestService.class);
        TestService service = new TestService();
        manager.registerService(key, service);
        Optional<TestService> found = manager.getService(key);
        assertTrue(found.isPresent());
        assertEquals(service, found.get());
    }

	@Test public void localizedContributionResolvesItsTitleOnEveryRead() {
		final String[] title = { "Deutsch" };
		UiContributionSpec contribution = UiContributionSpec.localized(PluginId.of("sample.plugin"),
				"menu", () -> title[0], UiContributionType.MENU, 10, "settings",
				AccessRequirement.AUTHENTICATED, null, () -> { });
		assertEquals("Deutsch", contribution.title());
		title[0] = "English";
		assertEquals("English", contribution.title());
	}

    @Test(expected = IllegalStateException.class)
    public void typedRegistryRejectsDuplicateKeys() {
        PluginManagerImpl manager = new PluginManagerImpl("unused");
        ServiceKey<TestService> key = ServiceKey.of("test.service", TestService.class);
        manager.registerService(key, new TestService());
        manager.registerService(key, new TestService());
    }

    private java.util.List<File> listDescriptors(File root) {
        java.util.List<File> result = new java.util.ArrayList<File>();
        File[] children = root.listFiles();
        if (children == null) return result;
        for (File child : children) {
            if (child.isDirectory()) result.addAll(listDescriptors(child));
            else if (child.getName().endsWith(".yaml")) result.add(child);
        }
        return result;
    }

    private static final class TestService implements Service { }
}
