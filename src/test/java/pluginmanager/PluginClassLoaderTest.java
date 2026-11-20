package pluginmanager;

import static org.junit.Assert.assertNotNull;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;

public class PluginClassLoaderTest {
    @Test public void closeReleasesLoadedJar() throws Exception {
        Path plugin = Files.createTempDirectory("companyware-plugin-loader-");
        Path library = Files.createDirectories(plugin.resolve("lib")).resolve("sample.jar");
        try (OutputStream stream = Files.newOutputStream(library);
                ZipOutputStream archive = new ZipOutputStream(stream)) {
            archive.putNextEntry(new ZipEntry("sample.txt"));
            archive.write(new byte[] { 1 });
            archive.closeEntry();
        }

        PluginClassLoader loader = new PluginClassLoader();
        try {
            loader.loadPlugin(plugin.toFile());
            assertNotNull(loader.getResource("sample.txt"));
        } finally {
            loader.close();
        }
        Files.delete(library);
        Files.delete(plugin.resolve("lib"));
        Files.delete(plugin);
    }

    @Test public void packageLoaderOwnsExactlyOneJarAndReleasesItOnClose() throws Exception {
        Path plugin = Files.createTempDirectory("companyware-package-loader-");
        Path library = plugin.resolve("plugin.jar");
        try (OutputStream stream = Files.newOutputStream(library);
                ZipOutputStream archive = new ZipOutputStream(stream)) {
            archive.putNextEntry(new ZipEntry("package-resource.txt"));
            archive.write(new byte[] { 1 });
            archive.closeEntry();
        }
        PackagePluginClassLoader loader = new PackagePluginClassLoader(library, getClass().getClassLoader());
        try { assertNotNull(loader.getResource("package-resource.txt")); }
        finally { loader.close(); }
        Files.delete(library);
        Files.delete(plugin);
    }
}
