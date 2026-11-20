package pluginmanager.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import platform.api.plugin.PluginDependency;
import platform.api.plugin.PluginDescriptor;
import platform.api.plugin.PluginDiagnostic;
import platform.api.plugin.PluginId;
import platform.api.plugin.PluginHealth;
import platform.api.plugin.PluginState;

/** Deterministic dependency resolution and failure-isolated lifecycle execution. */
public final class PluginLifecycleEngine {
    private final Map<PluginId, PluginDefinition> definitions = new LinkedHashMap<PluginId, PluginDefinition>();
    private final Map<PluginId, PluginState> states = new LinkedHashMap<PluginId, PluginState>();
    private final Map<PluginId, PluginRuntime> started = new LinkedHashMap<PluginId, PluginRuntime>();
    private final Set<PluginId> activating = new HashSet<PluginId>();
    private final List<PluginDiagnostic> diagnostics = new ArrayList<PluginDiagnostic>();
    private List<PluginDefinition> resolvedOrder = Collections.emptyList();

    public void discover(Collection<PluginDefinition> candidates) {
        if (!definitions.isEmpty()) throw new IllegalStateException("Lifecycle engine is already initialized");
        discoverAdditional(candidates);
    }

    /** Adds newly installed definitions without disturbing already running plugins. */
    public void discoverAdditional(Collection<PluginDefinition> candidates) {
        for (PluginDefinition candidate : candidates) {
            PluginId id = candidate.getDescriptor().getId();
            if (definitions.containsKey(id)) {
                throw new IllegalArgumentException("Duplicate plugin id: " + id);
            }
            definitions.put(id, candidate);
            states.put(id, PluginState.DISCOVERED);
            states.put(id, PluginState.INSTALLED);
        }
    }

    /** Replaces an inactive definition; callers can roll back with the returned value. */
    public PluginDefinition replaceDefinition(PluginDefinition replacement) {
        PluginId id = replacement.getDescriptor().getId();
        PluginDefinition previous = definitions.get(id);
        if (previous == null) throw new IllegalArgumentException("Unknown plugin: " + id);
        if (states.get(id) == PluginState.STARTED)
            throw new IllegalStateException("Active plugin must be stopped before update: " + id);
        definitions.put(id, replacement);
        states.put(id, PluginState.INSTALLED);
        clearDiagnostics(id);
        return previous;
    }

    /** Removes an inactive leaf definition from the graph. */
    public PluginDefinition removeDefinition(PluginId id) {
        PluginDefinition existing = definitions.get(id);
        if (existing == null) throw new IllegalArgumentException("Unknown plugin: " + id);
        if (states.get(id) == PluginState.STARTED)
            throw new IllegalStateException("Active plugin must be stopped before uninstall: " + id);
        for (PluginDefinition definition : definitions.values()) {
            if (definition == existing) continue;
            for (PluginDependency dependency : definition.getDescriptor().getDependencies()) {
                if (dependency.getPluginId().equals(id))
                    throw new IllegalStateException("Installed plugin depends on " + id + ": "
                            + definition.getDescriptor().getId());
            }
        }
        definitions.remove(id);
        states.remove(id);
        clearDiagnostics(id);
        List<PluginDefinition> remaining = new ArrayList<PluginDefinition>(resolvedOrder);
        remaining.remove(existing);
        resolvedOrder = Collections.unmodifiableList(remaining);
        return existing;
    }

    public PluginDescriptor descriptorOf(PluginId id) {
        PluginDefinition definition = definitions.get(id);
        return definition == null ? null : definition.getDescriptor();
    }

    private void clearDiagnostics(PluginId id) {
        for (Iterator<PluginDiagnostic> iterator = diagnostics.iterator(); iterator.hasNext();) {
            if (iterator.next().getPluginId().equals(id)) iterator.remove();
        }
    }

    public List<PluginDescriptor> resolve() {
        Map<PluginId, Integer> incoming = new HashMap<PluginId, Integer>();
        Map<PluginId, List<PluginId>> dependents = new HashMap<PluginId, List<PluginId>>();
        for (PluginId id : definitions.keySet()) {
            incoming.put(id, 0);
            dependents.put(id, new ArrayList<PluginId>());
        }
        for (PluginDefinition definition : definitions.values()) {
            PluginId id = definition.getDescriptor().getId();
            for (PluginDependency dependency : definition.getDescriptor().getDependencies()) {
                PluginDefinition provider = definitions.get(dependency.getPluginId());
                if (provider == null) {
                    fail(id, "Missing dependency " + dependency.getPluginId());
                } else if (provider.getDescriptor().getVersion().compareTo(dependency.getMinimumVersion()) < 0) {
                    fail(id, "Dependency " + dependency.getPluginId() + " requires at least " + dependency.getMinimumVersion());
                } else {
                    incoming.put(id, incoming.get(id) + 1);
                    dependents.get(dependency.getPluginId()).add(id);
                }
            }
        }
        propagateFailedDependencies();

        List<PluginDefinition> ready = new ArrayList<PluginDefinition>();
        for (PluginDefinition definition : definitions.values()) {
            PluginId id = definition.getDescriptor().getId();
            if (states.get(id) != PluginState.FAILED && incoming.get(id) == 0) ready.add(definition);
        }
        Collections.sort(ready, ORDER);
        List<PluginDefinition> order = new ArrayList<PluginDefinition>();
        while (!ready.isEmpty()) {
            PluginDefinition next = ready.remove(0);
            order.add(next);
            for (PluginId dependent : dependents.get(next.getDescriptor().getId())) {
                int remaining = incoming.get(dependent) - 1;
                incoming.put(dependent, remaining);
                if (remaining == 0 && states.get(dependent) != PluginState.FAILED) {
                    ready.add(definitions.get(dependent));
                    Collections.sort(ready, ORDER);
                }
            }
        }
        for (PluginDefinition definition : definitions.values()) {
            PluginId id = definition.getDescriptor().getId();
            if (states.get(id) != PluginState.FAILED && !order.contains(definition)) {
                fail(id, "Dependency cycle detected");
            }
        }
        propagateFailedDependencies();
        List<PluginDefinition> valid = new ArrayList<PluginDefinition>();
        for (PluginDefinition definition : order) {
            if (states.get(definition.getDescriptor().getId()) != PluginState.FAILED) {
                if (states.get(definition.getDescriptor().getId()) != PluginState.STARTED)
                    states.put(definition.getDescriptor().getId(), PluginState.RESOLVED);
                valid.add(definition);
            }
        }
        resolvedOrder = Collections.unmodifiableList(valid);
        List<PluginDescriptor> result = new ArrayList<PluginDescriptor>();
        for (PluginDefinition definition : resolvedOrder) result.add(definition.getDescriptor());
        return Collections.unmodifiableList(result);
    }

    public void startResolved() {
        Set<PluginId> all = new HashSet<PluginId>();
        for (PluginDefinition definition : resolvedOrder) all.add(definition.getDescriptor().getId());
        startResolved(all);
    }

    public void startResolved(Set<PluginId> enabled) {
        for (PluginDefinition definition : resolvedOrder) {
            PluginId id = definition.getDescriptor().getId();
            if (!enabled.contains(id)) continue;
            if (hasFailedDependency(definition.getDescriptor())) {
                fail(id, "A required plugin failed to start");
                continue;
            }
            try { activate(id); } catch (RuntimeException failure) { /* diagnostic retained */ }
        }
    }

    public void activate(PluginId id) {
        PluginDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unknown plugin: " + id);
        if (states.get(id) == PluginState.STARTED) return;
        if (!activating.add(id)) return;
        try {
            activateResolved(id, definition);
        } finally {
            activating.remove(id);
        }
    }

    private void activateResolved(PluginId id, PluginDefinition definition) {
        if (!resolvedOrder.contains(definition)) throw new IllegalStateException("Plugin is not resolved: " + id);
        for (PluginDependency dependency : definition.getDescriptor().getDependencies()) {
            if (states.get(dependency.getPluginId()) != PluginState.STARTED) {
                String message = "Required plugin is not active: " + dependency.getPluginId();
                fail(id, message);
                throw new IllegalStateException(message);
            }
        }
        PluginRuntime runtime = null;
        try {
            runtime = definition.createRuntime();
            runtime.start();
            started.put(id, runtime);
            states.put(id, PluginState.STARTED);
        } catch (Exception exception) {
            closeAfterFailedStart(runtime, id);
            fail(id, "Startup failed: " + safeMessage(exception));
            throw new IllegalStateException("Plugin startup failed: " + id + ": " + safeMessage(exception), exception);
        }
    }

    public void deactivate(PluginId id) {
        if (!started.containsKey(id)) return;
        for (PluginDefinition definition : definitions.values()) {
            if (states.get(definition.getDescriptor().getId()) != PluginState.STARTED) continue;
            for (PluginDependency dependency : definition.getDescriptor().getDependencies()) {
                if (dependency.getPluginId().equals(id))
                    throw new IllegalStateException("Active plugin depends on " + id + ": "
                            + definition.getDescriptor().getId());
            }
        }
        PluginRuntime runtime = started.get(id);
        try {
            runtime.stop();
            started.remove(id);
            states.put(id, PluginState.STOPPED);
        } catch (Exception exception) {
            fail(id, "Cleanup failed: " + safeMessage(exception));
            throw new IllegalStateException("Plugin cleanup failed: " + id + ": " + safeMessage(exception), exception);
        }
    }

    public void stopAll() {
        List<PluginId> ids = new ArrayList<PluginId>(started.keySet());
        Collections.reverse(ids);
        for (PluginId id : ids) {
            try { deactivate(id); } catch (RuntimeException failure) { /* diagnostic retained */ }
        }
        started.clear();
    }

    public PluginState stateOf(PluginId id) { return states.get(id); }
    public List<PluginDiagnostic> diagnostics() { return Collections.unmodifiableList(diagnostics); }

    public List<PluginHealth> health() {
        List<PluginHealth> result = new ArrayList<PluginHealth>();
        for (Map.Entry<PluginId, PluginState> entry : states.entrySet()) {
            String message = "";
            for (int index = diagnostics.size() - 1; index >= 0; index--) {
                PluginDiagnostic diagnostic = diagnostics.get(index);
                if (diagnostic.getPluginId().equals(entry.getKey())) {
                    message = diagnostic.getMessage();
                    break;
                }
            }
            result.add(new PluginHealth(entry.getKey(), entry.getValue(), message));
        }
        return Collections.unmodifiableList(result);
    }

    /** Activation changes are applied on the next application start. */
    public void markUninstallPending(PluginId id) {
        PluginState state = states.get(id);
        if (state == null) throw new IllegalArgumentException("Unknown plugin: " + id);
        states.put(id, PluginState.UNINSTALL_PENDING);
    }

    private boolean hasFailedDependency(PluginDescriptor descriptor) {
        for (PluginDependency dependency : descriptor.getDependencies()) {
            if (states.get(dependency.getPluginId()) == PluginState.FAILED) return true;
        }
        return false;
    }

    private void propagateFailedDependencies() {
        boolean changed;
        do {
            changed = false;
            for (PluginDefinition definition : definitions.values()) {
                PluginId id = definition.getDescriptor().getId();
                if (states.get(id) != PluginState.FAILED && hasFailedDependency(definition.getDescriptor())) {
                    fail(id, "A required plugin could not be resolved");
                    changed = true;
                }
            }
        } while (changed);
    }

    private void closeAfterFailedStart(PluginRuntime runtime, PluginId id) {
        if (runtime == null) return;
        try { runtime.stop(); }
        catch (Exception cleanup) { diagnostics.add(new PluginDiagnostic(id, PluginState.FAILED, "Rollback failed: " + safeMessage(cleanup))); }
    }

    private void fail(PluginId id, String message) {
        states.put(id, PluginState.FAILED);
        diagnostics.add(new PluginDiagnostic(id, PluginState.FAILED, message));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? exception.getClass().getSimpleName() : message;
    }

    private static final Comparator<PluginDefinition> ORDER = new Comparator<PluginDefinition>() {
        @Override public int compare(PluginDefinition left, PluginDefinition right) {
            int priority = Integer.compare(left.getDescriptor().getPriority(), right.getDescriptor().getPriority());
            return priority != 0 ? priority : left.getDescriptor().getId().toString().compareTo(right.getDescriptor().getId().toString());
        }
    };
}
