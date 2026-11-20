package pluginmanager.lifecycle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import platform.api.plugin.PluginHealth;
import platform.api.plugin.PluginDependency;
import platform.api.plugin.PluginDescriptor;
import platform.api.plugin.PluginId;
import platform.api.plugin.PluginState;
import platform.api.version.SemanticVersion;

public class PluginLifecycleEngineTest {
    @Test public void dependenciesOverridePriorityAndTiesUseStableIds() {
        List<String> calls = new ArrayList<String>();
        PluginDefinition base = definition("example.base", "1.0.0", 100, calls);
        PluginDefinition alpha = definition("example.alpha", "1.0.0", 1, calls, dependency("example.base", "1.0.0"));
        PluginDefinition beta = definition("example.beta", "1.0.0", 1, calls);
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(alpha, base, beta));
        List<PluginDescriptor> order = engine.resolve();
        assertEquals("example.beta", order.get(0).getId().toString());
        assertEquals("example.base", order.get(1).getId().toString());
        assertEquals("example.alpha", order.get(2).getId().toString());
        engine.startResolved();
        assertEquals(Arrays.asList("start:example.beta", "start:example.base", "start:example.alpha"), calls);
    }

    @Test public void healthReportsStartedAndFailedPluginsWithoutExceptions() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(
                definition("example.healthy", "1.0.0", 1, calls),
                definition("example.broken", "1.0.0", 2, calls, dependency("example.missing", "1.0.0"))));
        engine.resolve();
        engine.startResolved();
        List<PluginHealth> health = engine.health();
        assertEquals(2, health.size());
        assertEquals(PluginState.STARTED, health.get(0).getState());
        assertTrue(health.get(0).isHealthy());
        assertEquals(PluginState.FAILED, health.get(1).getState());
        assertTrue(health.get(1).getMessage().contains("Missing dependency"));
    }

    @Test public void missingAndIncompatibleDependenciesFailWithoutCreatingRuntime() {
        AtomicInteger creations = new AtomicInteger();
        PluginDefinition missing = countingDefinition("example.missing", "1.0.0", creations, dependency("not.installed", "1.0.0"));
        PluginDefinition provider = countingDefinition("example.provider", "1.0.0", creations);
        PluginDefinition incompatible = countingDefinition("example.consumer", "1.0.0", creations, dependency("example.provider", "2.0.0"));
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(missing, provider, incompatible));
        engine.resolve(); engine.startResolved();
        assertEquals(PluginState.FAILED, engine.stateOf(PluginId.of("example.missing")));
        assertEquals(PluginState.FAILED, engine.stateOf(PluginId.of("example.consumer")));
        assertEquals(1, creations.get());
    }

    @Test public void cyclesAndTheirDependentsAreRejected() {
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(
            definition("example.a", "1.0.0", 0, new ArrayList<String>(), dependency("example.b", "1.0.0")),
            definition("example.b", "1.0.0", 0, new ArrayList<String>(), dependency("example.a", "1.0.0")),
            definition("example.c", "1.0.0", 0, new ArrayList<String>(), dependency("example.a", "1.0.0"))));
        assertTrue(engine.resolve().isEmpty());
        assertEquals(PluginState.FAILED, engine.stateOf(PluginId.of("example.c")));
    }

    @Test public void failedStartupIsRolledBackAndIndependentPluginsSurvive() {
        List<String> calls = new ArrayList<String>();
        PluginDefinition good = definition("example.good", "1.0.0", 0, calls);
        PluginDefinition broken = new PluginDefinition(descriptor("example.broken", "1.0.0", 1), () -> new PluginRuntime() {
            public void start() { calls.add("start:broken"); throw new IllegalStateException("boom"); }
            public void stop() { calls.add("rollback:broken"); }
        });
        PluginDefinition child = definition("example.child", "1.0.0", 2, calls, dependency("example.broken", "1.0.0"));
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(good, broken, child)); engine.resolve(); engine.startResolved();
        assertEquals(PluginState.STARTED, engine.stateOf(PluginId.of("example.good")));
        assertEquals(PluginState.FAILED, engine.stateOf(PluginId.of("example.broken")));
        assertEquals(PluginState.FAILED, engine.stateOf(PluginId.of("example.child")));
        assertTrue(calls.contains("rollback:broken"));
    }

    @Test public void shutdownUsesReverseStartOrder() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(definition("example.a", "1.0.0", 1, calls), definition("example.b", "1.0.0", 2, calls)));
        engine.resolve(); engine.startResolved(); engine.stopAll();
        assertEquals(Arrays.asList("start:example.a", "start:example.b", "stop:example.b", "stop:example.a"), calls);
    }

    @Test public void inactivePluginCanStartAndStopAtRuntime() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Collections.singletonList(definition("example.live", "1.0.0", 1, calls)));
        engine.resolve();
        engine.startResolved(Collections.<PluginId>emptySet());
        assertEquals(PluginState.RESOLVED, engine.stateOf(PluginId.of("example.live")));
        engine.activate(PluginId.of("example.live"));
        assertEquals(PluginState.STARTED, engine.stateOf(PluginId.of("example.live")));
        engine.deactivate(PluginId.of("example.live"));
        assertEquals(PluginState.STOPPED, engine.stateOf(PluginId.of("example.live")));
        assertEquals(Arrays.asList("start:example.live", "stop:example.live"), calls);
    }

    @Test public void newlyDiscoveredPluginResolvesWithoutRestartingRunningPlugins() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Collections.singletonList(definition("example.base", "1.0.0", 1, calls)));
        engine.resolve();
        engine.startResolved();

        engine.discoverAdditional(Collections.singletonList(definition("example.new", "1.0.0", 2, calls,
                dependency("example.base", "1.0.0"))));
        engine.resolve();

        assertEquals(PluginState.STARTED, engine.stateOf(PluginId.of("example.base")));
        assertEquals(PluginState.RESOLVED, engine.stateOf(PluginId.of("example.new")));
        engine.activate(PluginId.of("example.new"));
        assertEquals(Arrays.asList("start:example.base", "start:example.new"), calls);
    }

    @Test public void reentrantActivationStartsPluginOnlyOnce() {
        final PluginLifecycleEngine engine = new PluginLifecycleEngine();
        final PluginId id = PluginId.of("example.reentrant");
        final AtomicInteger starts = new AtomicInteger();
        PluginDefinition definition = new PluginDefinition(descriptor(id.toString(), "1.0.0", 1), () -> new PluginRuntime() {
            public void start() {
                starts.incrementAndGet();
                engine.activate(id);
            }
            public void stop() { }
        });
        engine.discover(Collections.singletonList(definition));
        engine.resolve();

        engine.activate(id);

        assertEquals(1, starts.get());
        assertEquals(PluginState.STARTED, engine.stateOf(id));
    }

    @Test(expected = IllegalStateException.class)
    public void activeDependencyCannotBeStoppedBeforeItsConsumer() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(definition("example.base", "1.0.0", 1, calls),
                definition("example.child", "1.0.0", 2, calls, dependency("example.base", "1.0.0"))));
        engine.resolve(); engine.startResolved();
        engine.deactivate(PluginId.of("example.base"));
    }

    @Test public void inactiveDefinitionCanBeReplacedAndResolvedAtNewVersion() {
        List<String> calls = new ArrayList<String>();
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        PluginId id = PluginId.of("example.update");
        engine.discover(Collections.singletonList(definition(id.value(), "1.0.0", 1, calls)));
        engine.resolve();
        engine.replaceDefinition(definition(id.value(), "2.0.0", 1, calls));
        engine.resolve();
        assertEquals("2.0.0", engine.descriptorOf(id).getVersion().toString());
        assertEquals(PluginState.RESOLVED, engine.stateOf(id));
    }

    @Test public void inactiveLeafCanBeRemovedFromLifecycleGraph() {
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        PluginId id = PluginId.of("example.remove");
        engine.discover(Collections.singletonList(definition(id.value(), "1.0.0", 1, new ArrayList<String>())));
        engine.resolve();
        engine.removeDefinition(id);
        assertEquals(null, engine.descriptorOf(id));
        assertEquals(null, engine.stateOf(id));
    }

    @Test(expected = IllegalStateException.class)
    public void installedDependentPreventsProviderRemoval() {
        PluginLifecycleEngine engine = new PluginLifecycleEngine();
        engine.discover(Arrays.asList(
                definition("example.provider", "1.0.0", 1, new ArrayList<String>()),
                definition("example.consumer", "1.0.0", 2, new ArrayList<String>(),
                        dependency("example.provider", "1.0.0"))));
        engine.resolve();
        engine.removeDefinition(PluginId.of("example.provider"));
    }

    private static PluginDefinition definition(String id, String version, int priority, List<String> calls, PluginDependency... dependencies) {
        return new PluginDefinition(descriptor(id, version, priority, dependencies), () -> new PluginRuntime() {
            public void start() { calls.add("start:" + id); }
            public void stop() { calls.add("stop:" + id); }
        });
    }
    private static PluginDefinition countingDefinition(String id, String version, AtomicInteger counter, PluginDependency... dependencies) {
        return new PluginDefinition(descriptor(id, version, 0, dependencies), () -> {
            counter.incrementAndGet(); return new PluginRuntime() { public void start() { } public void stop() { } };
        });
    }
    private static PluginDescriptor descriptor(String id, String version, int priority, PluginDependency... dependencies) {
        return new PluginDescriptor(PluginId.of(id), id, "test", SemanticVersion.parse(version), SemanticVersion.parse("1.0.0"),
            "example.Plugin", priority, Arrays.asList(dependencies), Collections.emptySet());
    }
    private static PluginDependency dependency(String id, String minimum) {
        return new PluginDependency(PluginId.of(id), SemanticVersion.parse(minimum));
    }
}
