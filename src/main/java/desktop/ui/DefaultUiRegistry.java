package desktop.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import platform.api.ui.UiContributionSpec;
import platform.api.ui.UiContributionType;
import platform.api.ui.UiRegistry;

/** EDT-confined registry used by the Swing shell. */
public final class DefaultUiRegistry implements UiRegistry {
    private final Map<String, UiContributionSpec> contributions = new LinkedHashMap<String, UiContributionSpec>();
    private final List<Runnable> listeners = new ArrayList<Runnable>();

    @Override public AutoCloseable register(final UiContributionSpec contribution) {
        SwingEdt.run(new Runnable() { public void run() {
            String key = key(contribution);
            if (contributions.containsKey(key)) throw new IllegalStateException("Duplicate UI contribution: " + key);
            contributions.put(key, contribution);
            changed();
        }});
        final AtomicBoolean open = new AtomicBoolean(true);
        return new AutoCloseable() { public void close() {
            if (!open.compareAndSet(true, false)) return;
            SwingEdt.run(new Runnable() { public void run() { contributions.remove(key(contribution)); changed(); }});
        }};
    }

    @Override public List<UiContributionSpec> contributions(final UiContributionType type) {
        return SwingEdt.call(() -> {
            List<UiContributionSpec> result = new ArrayList<UiContributionSpec>();
            for (UiContributionSpec item : contributions.values()) if (item.type() == type) result.add(item);
            Collections.sort(result, Comparator.comparingInt(UiContributionSpec::order)
                    .thenComparing(item -> item.owner().value()).thenComparing(UiContributionSpec::id));
            return Collections.unmodifiableList(result);
        });
    }

    @Override public AutoCloseable onChange(final Runnable listener) {
        SwingEdt.run(() -> listeners.add(listener));
        final AtomicBoolean open = new AtomicBoolean(true);
        return () -> { if (open.compareAndSet(true, false)) SwingEdt.run(() -> listeners.remove(listener)); };
    }

    private String key(UiContributionSpec item) { return item.owner() + ":" + item.type() + ":" + item.id(); }
    private void changed() { for (Runnable listener : new ArrayList<Runnable>(listeners)) listener.run(); }
}
