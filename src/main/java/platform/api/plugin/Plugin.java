package platform.api.plugin;

/** Entry point implemented by plugins compiled against SDK version 1. */
public interface Plugin {
    void initialize(PluginContext context) throws Exception;

    /** Optional cleanup hook. Simple plugins do not need to implement it. */
    default void stop() throws Exception { }
}
