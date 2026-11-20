package pluginmanager.packaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import pluginmanager.PluginDescriptorReader;
import platform.api.plugin.PluginDescriptor;

/** Validates the complete archive before any file is extracted or plugin class is loaded. */
public final class PluginPackageInspector {
    public static final String DESCRIPTOR = "plugin.yaml";
    public static final String PLUGIN_JAR = "plugin.jar";
    private static final int BUFFER_SIZE = 8192;

    private final PluginPackagePolicy policy;

    public PluginPackageInspector() { this(PluginPackagePolicy.DEFAULT); }
    public PluginPackageInspector(PluginPackagePolicy policy) { this.policy = policy; }

    public PluginPackageInspection inspect(Path packageFile) throws IOException {
        if (packageFile == null || !Files.isRegularFile(packageFile)) {
            throw new IllegalArgumentException("Plugin package does not exist: " + packageFile);
        }
        String filename = packageFile.getFileName().toString().toLowerCase();
        if (!filename.endsWith(".cwp")) throw new IllegalArgumentException("Plugin package must use the .cwp extension");

        List<String> names = new ArrayList<String>();
        Set<String> uniqueNames = new HashSet<String>();
        byte[] descriptorBytes = null;
        long total = 0;
        boolean pluginJarFound = false;
        try (ZipFile archive = new ZipFile(packageFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (++count > policy.maximumEntries()) reject("Package contains too many entries");
                String name = safeRelativeName(entry.getName());
                if (!uniqueNames.add(name)) reject("Duplicate package entry: " + name);
                names.add(name);
                if (entry.isDirectory()) continue;
                ByteArrayOutputStream descriptor = DESCRIPTOR.equals(name) ? new ByteArrayOutputStream() : null;
                long actual = consume(archive.getInputStream(entry), descriptor, policy.maximumEntryBytes());
                total += actual;
                if (total > policy.maximumTotalBytes()) reject("Package is larger than the allowed uncompressed size");
                long compressed = entry.getCompressedSize();
                if (compressed == 0 && actual > 0) reject("Invalid compressed size for " + name);
                if (compressed > 0 && actual / compressed > policy.maximumCompressionRatio()) {
                    reject("Suspicious compression ratio for " + name);
                }
                if (descriptor != null) descriptorBytes = descriptor.toByteArray();
                if (PLUGIN_JAR.equals(name)) pluginJarFound = true;
            }
        }
        if (descriptorBytes == null) reject("Package must contain plugin.yaml at its root");
        if (!pluginJarFound) reject("Package must contain plugin.jar at its root");
        PluginDescriptor descriptor = PluginDescriptorReader.read(
                new ByteArrayInputStream(descriptorBytes), packageFile + "!/" + DESCRIPTOR);
        return new PluginPackageInspection(descriptor, sha256(packageFile), names, total);
    }

    static String safeRelativeName(String value) {
        if (value == null || value.trim().isEmpty()) reject("Empty package entry name");
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")) reject("Absolute package path: " + value);
        String[] parts = normalized.split("/", -1);
        for (String part : parts) {
            if ("..".equals(part) || ".".equals(part) || part.indexOf('\0') >= 0) reject("Unsafe package path: " + value);
        }
        return normalized;
    }

    private static long consume(InputStream input, ByteArrayOutputStream copy, long maximum) throws IOException {
        try (InputStream stream = input) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long count = 0;
            for (int read; (read = stream.read(buffer)) != -1;) {
                count += read;
                if (count > maximum) reject("Package entry exceeds the allowed size");
                if (copy != null) copy.write(buffer, 0, read);
            }
            return count;
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                for (int read; (read = input.read(buffer)) != -1;) digest.update(buffer, 0, read);
            }
            StringBuilder value = new StringBuilder();
            for (byte item : digest.digest()) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static void reject(String message) { throw new IllegalArgumentException(message); }
}
