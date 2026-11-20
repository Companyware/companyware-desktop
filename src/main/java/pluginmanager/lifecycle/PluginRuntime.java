package pluginmanager.lifecycle;

/** Internal adapter around legacy and SDK-v1 plugin entry points. */
public interface PluginRuntime {
    void start() throws Exception;
    void stop() throws Exception;
}
