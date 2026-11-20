package platform.api.ui;

import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.JComponent;
import platform.api.plugin.PluginId;

/** Simple immutable contribution used by the convenience PluginContext methods. */
public final class UiContributionSpec implements UiContribution {
    private final PluginId owner;
    private final String id;
    private final String title;
	private final Supplier<String> titleSupplier;
    private final UiContributionType type;
    private final int order;
    private final String target;
    private final AccessRequirement access;
    private final Supplier<? extends JComponent> componentFactory;
    private final Runnable command;
    private final Icon icon;

    public UiContributionSpec(PluginId owner, String id, String title, UiContributionType type, int order,
            String target, AccessRequirement access, Supplier<? extends JComponent> componentFactory, Runnable command) {
		this(owner, id, title, type, order, target, access, componentFactory, command, null);
	}

    public UiContributionSpec(PluginId owner, String id, String title, UiContributionType type, int order,
            String target, AccessRequirement access, Supplier<? extends JComponent> componentFactory,
            Runnable command, Icon icon) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.id = text(id, "id");
        this.title = text(title, "title");
		this.titleSupplier = null;
        this.type = Objects.requireNonNull(type, "type");
        this.order = order;
        this.target = target;
        this.access = access == null ? AccessRequirement.PUBLIC : access;
        this.componentFactory = componentFactory;
        this.command = command;
        this.icon = icon;
        validateAction(type, componentFactory, command);
    }

	private UiContributionSpec(PluginId owner, String id, Supplier<String> titleSupplier,
			UiContributionType type, int order, String target, AccessRequirement access,
			Supplier<? extends JComponent> componentFactory, Runnable command, Icon icon) {
		this.owner = Objects.requireNonNull(owner, "owner");
		this.id = text(id, "id");
		this.title = null;
		this.titleSupplier = Objects.requireNonNull(titleSupplier, "titleSupplier");
		this.type = Objects.requireNonNull(type, "type");
		this.order = order;
		this.target = target;
		this.access = access == null ? AccessRequirement.PUBLIC : access;
		this.componentFactory = componentFactory;
		this.command = command;
		this.icon = icon;
		validateAction(type, componentFactory, command);
	}

	public static UiContributionSpec localized(PluginId owner, String id, Supplier<String> titleSupplier,
			UiContributionType type, int order, String target, AccessRequirement access,
			Supplier<? extends JComponent> componentFactory, Runnable command) {
		return new UiContributionSpec(owner, id, titleSupplier, type, order, target, access,
				componentFactory, command, null);
	}

	public static UiContributionSpec localized(PluginId owner, String id, Supplier<String> titleSupplier,
			UiContributionType type, int order, String target, AccessRequirement access,
			Supplier<? extends JComponent> componentFactory, Runnable command, Icon icon) {
		return new UiContributionSpec(owner, id, titleSupplier, type, order, target, access,
				componentFactory, command, icon);
	}

	private static void validateAction(UiContributionType type,
			Supplier<? extends JComponent> componentFactory, Runnable command) {
		if (type == UiContributionType.MENU && command == null)
			throw new IllegalArgumentException("MENU requires a command");
		if ((type == UiContributionType.VIEW || type == UiContributionType.DIALOG
				|| type == UiContributionType.SETTINGS_PAGE) && componentFactory == null)
			throw new IllegalArgumentException(type + " requires a component factory");
	}
    private static String text(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }
    public PluginId owner() { return owner; }
    public String id() { return id; }
	public String title() { return titleSupplier == null ? title : text(titleSupplier.get(), "title"); }
    public UiContributionType type() { return type; }
    public int order() { return order; }
    public String target() { return target; }
    public Icon icon() { return icon; }
    public AccessRequirement access() { return access; }
    public JComponent component() { return componentFactory == null ? null : componentFactory.get(); }
    public void execute() { if (command != null) command.run(); }
}
