package pluginmanager.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PluginPackageInstallerTest {
    private Path temporary;

    @Before public void createTemporaryDirectory() throws IOException { temporary = Files.createTempDirectory("companyware-package-"); }
    @After public void removeTemporaryDirectory() throws IOException {
        if (temporary == null || !Files.exists(temporary)) return;
        try (Stream<Path> paths = Files.walk(temporary)) {
            for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator) Files.deleteIfExists(path);
        }
    }

    @Test public void installsValidatedPackageAndStoresTrustAndChecksum() throws Exception {
        Path packageFile = packageWith("valid.cwp", "plugin.yaml", descriptor("sample.plugin"), "plugin.jar", "jar");
        Path installRoot = temporary.resolve("installed");
        PluginPackageInspection inspection = new PluginPackageInstaller().installTrustedLocal(packageFile, installRoot);
        Path installed = installRoot.resolve("sample.plugin");
        assertTrue(Files.isRegularFile(installed.resolve("plugin.yaml")));
        assertTrue(Files.isRegularFile(installed.resolve("plugin.jar")));
        assertTrue(Files.isRegularFile(installed.resolve(".companyware-package.properties")));
        assertEquals(64, inspection.getSha256().length());
    }

    @Test public void rejectsZipSlipWithoutChangingExistingInstallation() throws Exception {
        Path installRoot = temporary.resolve("installed");
        Path existing = installRoot.resolve("sample.plugin");
        Files.createDirectories(existing);
        Files.write(existing.resolve("marker.txt"), "unchanged".getBytes(StandardCharsets.UTF_8));
        Path packageFile = packageWith("attack.cwp", "plugin.yaml", descriptor("sample.plugin"),
                "plugin.jar", "jar", "../escaped.txt", "attack");
        expectRejected(packageFile, installRoot, "Unsafe package path");
        assertEquals("unchanged", new String(Files.readAllBytes(existing.resolve("marker.txt")), StandardCharsets.UTF_8));
        assertFalse(Files.exists(temporary.resolve("escaped.txt")));
    }

    @Test public void rejectsArchiveBombBeforeInstallation() throws Exception {
        Path packageFile = packageWith("bomb.cwp", "plugin.yaml", descriptor("sample.plugin"),
                "plugin.jar", repeat('a', 4096));
        PluginPackageInspector strict = new PluginPackageInspector(new PluginPackagePolicy(10, 512, 1024, 10));
        try {
            new PluginPackageInstaller(strict).installTrustedLocal(packageFile, temporary.resolve("installed"));
            fail("Expected package rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("allowed size"));
        }
        assertFalse(Files.exists(temporary.resolve("installed")));
    }

    @Test public void rejectsDescriptorIncompatibleWithCurrentApi() throws Exception {
        String incompatible = descriptor("sample.plugin").replace("requiredApiVersion: 1.0.0", "requiredApiVersion: 2.0.0");
        Path packageFile = packageWith("incompatible.cwp", "plugin.yaml", incompatible, "plugin.jar", "jar");
        expectRejected(packageFile, temporary.resolve("installed"), "incompatible API");
    }

    @Test public void restoresPreviousPackageWhenRuntimeCommitRejectsUpdate() throws Exception {
        Path installRoot = temporary.resolve("installed");
        Path existing = installRoot.resolve("sample.plugin");
        Files.createDirectories(existing);
        Files.write(existing.resolve("marker.txt"), "old".getBytes(StandardCharsets.UTF_8));
        Path packageFile = packageWith("update.cwp", "plugin.yaml", descriptor("sample.plugin"),
                "plugin.jar", "new");
        final AtomicBoolean rolledBack = new AtomicBoolean();
        try {
            new PluginPackageInstaller().installTrustedLocal(packageFile, installRoot,
                    new PluginPackageInstaller.InstallationCommit() {
                public void commit(PluginPackageInspection inspection, Path directory) {
                    throw new IllegalStateException("runtime rejected");
                }
                public void rollback() { rolledBack.set(true); }
            });
            fail("Expected transactional rollback");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("runtime rejected"));
        }
        assertEquals("old", new String(Files.readAllBytes(existing.resolve("marker.txt")), StandardCharsets.UTF_8));
        assertFalse(Files.exists(existing.resolve("plugin.jar")));
        assertTrue(rolledBack.get());
    }

    @Test public void removalRestoresDirectoryWhenRuntimeCommitFails() throws Exception {
        Path installRoot = temporary.resolve("installed");
        Path existing = installRoot.resolve("sample.plugin");
        Files.createDirectories(existing);
        Files.write(existing.resolve("marker.txt"), "old".getBytes(StandardCharsets.UTF_8));
        final AtomicBoolean rolledBack = new AtomicBoolean();
        try {
            new PluginPackageInstaller().removeInstalled(installRoot, "sample.plugin",
                    new PluginPackageInstaller.RemovalCommit() {
                public void commit(Path removed) { throw new IllegalStateException("database rejected"); }
                public void rollback() { rolledBack.set(true); }
            });
            fail("Expected transactional rollback");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("database rejected"));
        }
        assertTrue(Files.isRegularFile(existing.resolve("marker.txt")));
        assertTrue(rolledBack.get());
    }

    @Test public void removalDeletesCommittedPackageDirectory() throws Exception {
        Path installRoot = temporary.resolve("installed");
        Path existing = installRoot.resolve("sample.plugin");
        Files.createDirectories(existing);
        Files.write(existing.resolve("marker.txt"), "old".getBytes(StandardCharsets.UTF_8));
        new PluginPackageInstaller().removeInstalled(installRoot, "sample.plugin", removed -> { });
        assertFalse(Files.exists(existing));
    }

    private void expectRejected(Path packageFile, Path installRoot, String message) throws Exception {
        try {
            new PluginPackageInstaller().installTrustedLocal(packageFile, installRoot);
            fail("Expected package rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private Path packageWith(String filename, String... nameAndContent) throws IOException {
        Path result = temporary.resolve(filename);
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(result))) {
            for (int index = 0; index < nameAndContent.length; index += 2) {
                output.putNextEntry(new ZipEntry(nameAndContent[index]));
                output.write(nameAndContent[index + 1].getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        }
        return result;
    }

    private String descriptor(String id) {
        return "id: " + id + "\nname: Sample\nauthor: Test\nversion: 1.0.0\n"
                + "requiredApiVersion: 1.0.0\nentryPoint: sample.Plugin\npriority: 0\n"
                + "capabilities: []\ndependencies: []\n";
    }

    private String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
