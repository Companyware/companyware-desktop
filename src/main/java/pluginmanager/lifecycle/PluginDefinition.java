package pluginmanager.lifecycle;

import java.util.Objects;
import platform.api.plugin.PluginDescriptor;

/** Descriptor and lazy runtime factory; resolving dependencies never executes plugin code. */
public final class PluginDefinition {
    public interface Factory { PluginRuntime create() throws Exception; }

    private final PluginDescriptor descriptor;
    private final Factory factory;

    public PluginDefinition(PluginDescriptor descriptor, Factory factory) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public PluginDescriptor getDescriptor() { return descriptor; }
    public PluginRuntime createRuntime() throws Exception { return factory.create(); }
}
