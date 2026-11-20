package core;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Starts JVM shutdown away from Swing's event thread so EDT-bound cleanup can finish. */
public final class ApplicationShutdown {
    private static final AtomicBoolean REQUESTED = new AtomicBoolean(false);
    private static final AtomicReference<Runnable> CLEANUP =
            new AtomicReference<Runnable>(() -> { });

    private ApplicationShutdown() { }

    public static void configure(Runnable cleanup) {
        if (cleanup == null) throw new IllegalArgumentException("Cleanup is required");
        CLEANUP.set(cleanup);
    }

    public static void request() {
        if (!REQUESTED.compareAndSet(false, true)) return;
        Thread shutdown = new Thread(() -> {
            try {
                CLEANUP.get().run();
            } finally {
                System.exit(0);
            }
        }, "companyware-shutdown-request");
        shutdown.start();
    }
}
