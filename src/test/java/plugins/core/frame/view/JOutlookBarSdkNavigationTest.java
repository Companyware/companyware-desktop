package plugins.core.frame.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import org.junit.Test;
import platform.api.plugin.PluginId;
import platform.api.ui.AccessRequirement;
import platform.api.ui.CompanywareIcons;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;

public class JOutlookBarSdkNavigationTest {
    @Test public void groupedChildWithoutIconUsesTheStandardDocumentSymbol() {
        PluginId owner = PluginId.of("sample.group");
        UiContributionSpec group = navigation(owner, "group", "Group", 10, null);
        UiContributionSpec child = navigation(owner, "child", "Child", 11, "child.view");
        UiContributionSpec view = view(owner, "child.view", "Child", 11, new JLabel("Child"));
        JOutlookBar bar = new JOutlookBar();

        bar.replaceSdkNavigation(Arrays.asList(group, child), Collections.singletonList(view), component -> { });
        findButton(bar, "Group").doClick();

        JButton childButton = findButton(bar, "Child");
        assertNotNull(childButton);
        assertEquals(CompanywareIcons.DOCUMENT, childButton.getIcon());
        assertEquals(72, childButton.getPreferredSize().height);
    }

    @Test public void navigationOpensOwnedViewAndDisappearsWithRegistration() {
        PluginId owner = PluginId.of("sample.hello");
        JLabel panel = new JLabel("Hello World!");
        UiContributionSpec view = new UiContributionSpec(owner, "hello.view", "HelloWorld",
                UiContributionType.VIEW, 120, null, AccessRequirement.AUTHENTICATED,
                () -> panel, null);
        UiContributionSpec navigation = new UiContributionSpec(owner, "hello.navigation", "HelloWorld",
                UiContributionType.NAVIGATION, 120, "hello.view", AccessRequirement.AUTHENTICATED,
                null, null);
        AtomicReference<JComponent> shown = new AtomicReference<JComponent>();
        JOutlookBar bar = new JOutlookBar();

        bar.replaceSdkNavigation(Collections.singletonList(navigation),
                Collections.singletonList(view), shown::set);
        JButton button = findButton(bar, "HelloWorld");
        assertNotNull(button);
        button.doClick();
        assertEquals(panel, shown.get());

        bar.replaceSdkNavigation(Collections.<UiContributionSpec>emptyList(),
                Collections.<UiContributionSpec>emptyList(), shown::set);
        assertNull(findButton(bar, "HelloWorld"));
    }

    @Test public void rendersAllActivePluginNavigationContributions() {
        PluginId demoOwner = PluginId.of("companyware.demo");
        PluginId customerOwner = PluginId.of("mbdus.customer");
        JLabel demoPanel = new JLabel("Demo");
        JLabel customerPanel = new JLabel("Customer");
        UiContributionSpec demoView = view(demoOwner, "overview.view", "Demo overview", 100, demoPanel);
        UiContributionSpec customerView = view(customerOwner, "customer.view", "Customers", 110, customerPanel);
        UiContributionSpec demoNavigation = navigation(demoOwner, "overview.navigation",
                "Demo overview", 100, "overview.view");
        UiContributionSpec customerNavigation = navigation(customerOwner, "customer.navigation",
                "Customers", 110, "customer.view");
        JOutlookBar bar = new JOutlookBar();

        bar.replaceSdkNavigation(Arrays.asList(demoNavigation, customerNavigation),
                Arrays.asList(demoView, customerView), component -> { });

        assertNotNull(findButton(bar, "Demo overview"));
        assertNotNull(findButton(bar, "Customers"));
        Container primaryNavigation = (Container) ((BorderLayout) bar.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        Container footerArea = (Container) ((BorderLayout) bar.getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);
        assertNotNull(findButton(primaryNavigation, "Demo overview"));
        assertNotNull(findButton(primaryNavigation, "Customers"));
        assertNull(findButton(footerArea, "Demo overview"));
        assertNull(findButton(footerArea, "Customers"));
    }

    @Test public void rendersNavigationGroupLikeAnOutlookBarItem() {
        PluginId owner = PluginId.of("companyware.invoice");
        AtomicReference<JComponent> suppliedPanel = new AtomicReference<JComponent>();
        UiContributionSpec heading = navigation(owner, "invoice.group", "Invoices", 130, null);
        ImageIcon overviewIcon = new ImageIcon(new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB));
        UiContributionSpec overview = navigation(owner, "invoice.overview", "Overview", 131,
                "overview.view", overviewIcon);
        UiContributionSpec view = new UiContributionSpec(owner, "overview.view", "Overview",
                UiContributionType.VIEW, 131, null, AccessRequirement.AUTHENTICATED, () -> {
                    JComponent created = new JLabel("Overview");
                    suppliedPanel.set(created);
                    return created;
                }, null);
        PluginId homeOwner = PluginId.of("companyware.home");
        UiContributionSpec homeView = view(homeOwner, "home.view", "Home", 100, new JLabel("Home"));
        UiContributionSpec homeNavigation = navigation(homeOwner, "home.navigation", "Home", 100, "home.view");
        PluginId settingsOwner = PluginId.of("companyware.settings");
        UiContributionSpec settingsView = view(settingsOwner, "settings.view", "Settings", 200,
                new JLabel("Settings"));
        UiContributionSpec settingsNavigation = navigation(settingsOwner, "settings.navigation", "Settings", 200,
                "settings.view");
        AtomicReference<JComponent> shown = new AtomicReference<JComponent>();
        JOutlookBar bar = new JOutlookBar();

        bar.replaceSdkNavigation(Arrays.asList(homeNavigation, heading, overview, settingsNavigation),
                Arrays.asList(homeView, view, settingsView), component -> {
                    shown.set(component);
                    bar.selectSdkView(component);
                });

        JButton headingButton = findButton(bar, "Invoices");
        JButton homeButton = findButton(bar, "Home");
        JButton settingsButton = findButton(bar, "Settings");
        JButton overviewButton = findButton(bar, "Overview");
		bar.setFooterAction("Profile", "profile", event -> { });
		JButton profileButton = findButton(bar, "Profile");
		assertEquals(Integer.MAX_VALUE, profileButton.getMaximumSize().width);
		assertEquals(46, profileButton.getMaximumSize().height);
		bar.setSelectedEntry("profile");
		assertEquals(core.ui.CompanywareTheme.navigationSelected(), profileButton.getBackground());
        assertNotNull(headingButton);
		assertEquals(Boolean.TRUE, headingButton.getClientProperty("companyware.navigation.group"));
		assertNull(homeButton.getClientProperty("companyware.navigation.group"));
		assertEquals(java.awt.Cursor.HAND_CURSOR, homeButton.getCursor().getType());
		assertEquals(java.awt.Cursor.HAND_CURSOR, headingButton.getCursor().getType());
		assertEquals(Boolean.FALSE, homeButton.getClientProperty("companyware.navigation.active"));
		assertEquals(Boolean.FALSE, headingButton.getClientProperty("companyware.navigation.expanded"));
        assertEquals(homeButton.getPreferredSize().height, headingButton.getPreferredSize().height);
        assertEquals(settingsButton.getPreferredSize().height, headingButton.getPreferredSize().height);
        assertEquals(homeButton.getBackground(), headingButton.getBackground());
        assertEquals(homeButton.getBorder().getBorderInsets(homeButton),
                headingButton.getBorder().getBorderInsets(headingButton));
		homeButton.doClick();
		assertEquals(core.ui.CompanywareTheme.surface(), profileButton.getBackground());
		assertEquals(Boolean.TRUE, homeButton.getClientProperty("companyware.navigation.active"));
		assertEquals(Boolean.FALSE, headingButton.getClientProperty("companyware.navigation.active"));
		shown.set(null);
        assertNull(overviewButton);
        headingButton.doClick();
		assertEquals(Boolean.TRUE, headingButton.getClientProperty("companyware.navigation.expanded"));
        assertNull(shown.get());
        overviewButton = findButton(bar, "Overview");
        assertNotNull(overviewButton);
        assertEquals(overviewIcon, overviewButton.getIcon());
        assertEquals(72, overviewButton.getPreferredSize().height);
        assertEquals(javax.swing.SwingConstants.CENTER, overviewButton.getHorizontalTextPosition());
        assertEquals(javax.swing.SwingConstants.BOTTOM, overviewButton.getVerticalTextPosition());
        Container primaryNavigation = (Container) ((BorderLayout) bar.getLayout())
                .getLayoutComponent(BorderLayout.NORTH);
        assertEquals("Invoices", primaryNavigation.getComponent(1) instanceof JButton
                ? ((JButton) primaryNavigation.getComponent(1)).getText() : null);
        assertEquals(overviewButton.getParent(), primaryNavigation.getComponent(2));
        assertEquals("Settings", primaryNavigation.getComponent(3) instanceof JButton
                ? ((JButton) primaryNavigation.getComponent(3)).getText() : null);
        overviewButton.doClick();
        assertEquals(suppliedPanel.get(), shown.get());
		assertEquals(Boolean.TRUE, overviewButton.getClientProperty("companyware.navigation.activeChild"));
		assertEquals(core.ui.CompanywareTheme.navigationSelected(), overviewButton.getBackground());
		assertEquals(Boolean.TRUE, headingButton.getClientProperty("companyware.navigation.active"));
        assertEquals(javax.swing.SwingConstants.CENTER, overviewButton.getHorizontalAlignment());
		headingButton.doClick();
		assertEquals(Boolean.FALSE, headingButton.getClientProperty("companyware.navigation.expanded"));
		assertNull(findButton(bar, "Overview"));
		assertEquals(Boolean.FALSE, overviewButton.getClientProperty("companyware.navigation.activeChild"));
		assertEquals(core.ui.CompanywareTheme.sidebar(), overviewButton.getBackground());
		assertEquals(BorderFactory.createEmptyBorder(6, 8, 6, 8).getBorderInsets(overviewButton),
				overviewButton.getBorder().getBorderInsets(overviewButton));
		bar.selectSdkView(suppliedPanel.get());
		assertEquals(Boolean.FALSE, overviewButton.getClientProperty("companyware.navigation.activeChild"));
		headingButton.doClick();
		assertEquals(Boolean.FALSE, overviewButton.getClientProperty("companyware.navigation.activeChild"));
		overviewButton.doClick();
		assertEquals(Boolean.TRUE, overviewButton.getClientProperty("companyware.navigation.activeChild"));
    }

    private UiContributionSpec view(PluginId owner, String id, String title, int order,
            JComponent component) {
        return new UiContributionSpec(owner, id, title, UiContributionType.VIEW, order, null,
                AccessRequirement.AUTHENTICATED, () -> component, null);
    }

    private UiContributionSpec navigation(PluginId owner, String id, String title, int order,
            String target) {
        return new UiContributionSpec(owner, id, title, UiContributionType.NAVIGATION, order, target,
                AccessRequirement.AUTHENTICATED, null, null);
    }

    private UiContributionSpec navigation(PluginId owner, String id, String title, int order,
            String target, javax.swing.Icon icon) {
        return new UiContributionSpec(owner, id, title, UiContributionType.NAVIGATION, order, target,
                AccessRequirement.AUTHENTICATED, null, null, icon);
    }

    private JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton nested = findButton((Container) component, text);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
