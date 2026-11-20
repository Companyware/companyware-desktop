package pluginmanager.packaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Installs an inspected local package via staging and restores the previous version on failure. */
public final class PluginPackageInstaller {
    public interface InstallationCommit {
        void commit(PluginPackageInspection inspection, Path installedDirectory) throws Exception;
        default void rollback() throws Exception { }
    }

    public interface RemovalCommit {
        void commit(Path removedDirectory) throws Exception;
        default void rollback() throws Exception { }
    }

    private final PluginPackageInspector inspector;

    public PluginPackageInstaller() { this(new PluginPackageInspector()); }
    public PluginPackageInstaller(PluginPackageInspector inspector) { this.inspector = inspector; }

    public PluginPackageInspection installTrustedLocal(Path packageFile, Path installRoot) throws IOException {
        return installTrustedLocal(packageFile, installRoot, (inspection, installedDirectory) -> { });
    }

    public PluginPackageInspection installTrustedLocal(Path packageFile, Path installRoot,
            InstallationCommit commit) throws IOException {
        PluginPackageInspection inspection = inspector.inspect(packageFile);
        Files.createDirectories(installRoot);
        Path root = installRoot.toAbsolutePath().normalize();
        String id = inspection.getDescriptor().getId().value();
        Path target = root.resolve(id);
        Path staging = root.resolve(".staging-" + id + "-" + UUID.randomUUID());
        Path backup = root.resolve(".backup-" + id + "-" + UUID.randomUUID());
        boolean previousMoved = false;
        boolean installedMoved = false;
        boolean commitStarted = false;
        try {
            Files.createDirectory(staging);
            extract(packageFile, staging);
            writeMetadata(staging, inspection);
            if (Files.exists(target)) {
                move(target, backup);
                previousMoved = true;
            }
            move(staging, target);
            installedMoved = true;
            commitStarted = true;
            commit.commit(inspection, target);
            if (previousMoved) deleteTree(backup);
            return inspection;
        } catch (Exception failure) {
            if (commitStarted) {
                try { commit.rollback(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            try { deleteTree(staging); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            if (installedMoved) {
                try { deleteTree(target); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            if (previousMoved && Files.exists(backup)) {
                try { move(backup, target); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            rethrow(failure, "Plugin installation failed");
            throw new AssertionError("unreachable");
        }
    }

    public void removeInstalled(Path installRoot, String pluginId, RemovalCommit commit) throws IOException {
        Path root = installRoot.toAbsolutePath().normalize();
        Path target = root.resolve(pluginId).normalize();
        if (!target.getParent().equals(root) || !Files.isDirectory(target))
            throw new IllegalArgumentException("Installed plugin directory does not exist: " + pluginId);
        Path removed = root.resolve(".removing-" + pluginId + "-" + UUID.randomUUID());
        boolean moved = false;
        boolean commitStarted = false;
        try {
            move(target, removed);
            moved = true;
            commitStarted = true;
            commit.commit(removed);
            deleteTree(removed);
        } catch (Exception failure) {
            if (commitStarted) {
                try { commit.rollback(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            if (moved && Files.exists(removed)) {
                try { move(removed, target); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            rethrow(failure, "Plugin removal failed");
        }
    }

    private static void rethrow(Exception failure, String message) throws IOException {
        if (failure instanceof IOException) throw (IOException) failure;
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        throw new IOException(message, failure);
    }

    private static void extract(Path packageFile, Path staging) throws IOException {
        try (ZipFile archive = new ZipFile(packageFile.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = PluginPackageInspector.safeRelativeName(entry.getName());
                Path destination = staging.resolve(name).normalize();
                if (!destination.startsWith(staging)) throw new IllegalArgumentException("Unsafe package path: " + name);
                if (entry.isDirectory()) Files.createDirectories(destination);
                else {
                    Path parent = destination.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try (InputStream input = archive.getInputStream(entry);
                            OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
                        byte[] buffer = new byte[8192];
                        for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    private static void writeMetadata(Path staging, PluginPackageInspection inspection) throws IOException {
        Properties metadata = new Properties();
        metadata.setProperty("sha256", inspection.getSha256());
        metadata.setProperty("trust", "trusted-local");
        metadata.setProperty("id", inspection.getDescriptor().getId().value());
        metadata.setProperty("version", inspection.getDescriptor().getVersion().toString());
        try (OutputStream output = Files.newOutputStream(staging.resolve(".companyware-package.properties"),
                StandardOpenOption.CREATE_NEW)) {
            metadata.store(output, "Companyware plugin package metadata");
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException unsupported) { Files.move(source, target); }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }
}
