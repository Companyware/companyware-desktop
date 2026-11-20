package desktop.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;

final class SwingEdt {
    private SwingEdt() { }
    static void run(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) task.run();
        else try { SwingUtilities.invokeAndWait(task); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
        catch (InvocationTargetException failure) { throw propagate(failure.getCause()); }
    }
    static void runLater(Runnable task) { SwingUtilities.invokeLater(task); }
    static <T> T call(Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) try { return task.call(); } catch (Exception failure) { throw propagate(failure); }
        FutureTask<T> future = new FutureTask<T>(task);
        run(future);
        try { return future.get(); } catch (Exception failure) { throw propagate(failure.getCause()); }
    }
    private static RuntimeException propagate(Throwable failure) {
        return failure instanceof RuntimeException ? (RuntimeException) failure : new IllegalStateException(failure);
    }
}
