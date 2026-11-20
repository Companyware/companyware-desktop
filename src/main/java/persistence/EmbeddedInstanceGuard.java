package persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Prevents two local JVMs from opening the same embedded Derby database. */
public final class EmbeddedInstanceGuard implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private EmbeddedInstanceGuard(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static EmbeddedInstanceGuard acquire(PersistenceSettings settings) throws IOException {
        if (settings.getProfile() != PersistenceProfile.EMBEDDED) return new EmbeddedInstanceGuard(null, null);
        Path database = settings.getEmbeddedPath().toAbsolutePath().normalize();
        Path parent = database.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path lockPath = database.resolveSibling(database.getFileName() + ".companyware.lock");
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) throw alreadyRunning(database);
            return new EmbeddedInstanceGuard(channel, lock);
        } catch (OverlappingFileLockException failure) {
            closeQuietly(channel);
            throw alreadyRunning(database);
        } catch (IOException failure) {
            closeQuietly(channel);
            throw failure;
        }
    }

    private static EmbeddedDatabaseInUseException alreadyRunning(Path database) {
        return new EmbeddedDatabaseInUseException(database);
    }

    private static void closeQuietly(FileChannel channel) {
        try { channel.close(); } catch (IOException ignored) { }
    }

    @Override public void close() throws IOException {
        if (lock != null && lock.isValid()) lock.release();
        if (channel != null && channel.isOpen()) channel.close();
    }
}
