package platform.api.plugin;

/** Observable states of a plugin managed by the platform lifecycle. */
public enum PluginState {
    DISCOVERED,
    INSTALLED,
    RESOLVED,
    STARTED,
    STOPPED,
    FAILED,
    UNINSTALL_PENDING
}
