/*
 * Companyware
 * Copyright (c) 2026 Companyware contributors
 *
 * SPDX-License-Identifier: MIT
 * Licensed under the MIT License; see LICENSE in the project root.
 * This file is part of Companyware.
 */
package architecture;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import pluginmanager.packaging.PluginPackageInspection;
import pluginmanager.packaging.PluginPackageInspector;

/** Protects the AP-02 starter's public-SDK and explicit-approval boundaries. */
public final class PluginStarterArchitectureTest {
    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test public void desktopBuildPublishesOnlyThePublicApiClassifier() throws Exception {
        String pom = read("pom.xml");
        assertTrue(pom.contains("<id>public-plugin-sdk</id>"));
        assertTrue(pom.contains("<classifier>sdk</classifier>"));
        assertTrue(pom.contains("<include>platform/api/**</include>"));
    }

    @Test public void starterPinsSdkAndProducesTwoEntryCwp() throws Exception {
        String pom = read("tools/plugin-starter/pom.xml.template");
        String assembly = read("tools/plugin-starter/plugin-assembly.xml");
        assertTrue(pom.contains("<companyware.sdk.version>__SDK_VERSION__</companyware.sdk.version>"));
        assertTrue(pom.contains("<classifier>sdk</classifier>"));
        assertTrue(pom.contains("<requireJavaVersion><version>[1.8,1.9)</version>"));
        assertTrue(assembly.contains("<destName>plugin.yaml</destName>"));
        assertTrue(assembly.contains("<destName>plugin.jar</destName>"));
        assertFalse(assembly.contains("fileSet"));
    }

    @Test public void generatedExampleCannotImportDesktopInternals() throws Exception {
        String source = read("tools/plugin-starter/Plugin.java.template");
        assertTrue(source.contains("import platform.api.plugin.Plugin;"));
        assertFalse(source.contains("import core."));
        assertFalse(source.contains("import desktop."));
        assertFalse(source.contains("import pluginmanager."));
        assertFalse(source.contains("installTrustedLocal"));
        assertFalse(source.contains("activate"));
    }

    @Test public void generatorRefusesOverwriteAndDoesNotInstall() throws Exception {
        String generator = read("tools/new-plugin-project.ps1");
        assertTrue(generator.contains("Refusing to overwrite existing path"));
        assertFalse(generator.contains("installTrustedLocal"));
        assertFalse(generator.contains("PluginManager"));
    }

    @Test public void generatedDescriptorPassesTheExistingPackageInspector() throws Exception {
        String descriptor = read("tools/plugin-starter/plugin.yaml.template")
                .replace("__PLUGIN_ID__", "example.inventory")
                .replace("__DISPLAY_NAME_YAML__", "Inventory & \"Quotes\": O''Brien")
                .replace("__AUTHOR_YAML__", "Example Developer")
                .replace("__PLUGIN_VERSION__", "1.0.0")
                .replace("__SDK_API_VERSION__", "1.0.0")
                .replace("__JAVA_PACKAGE__", "example.companyware.inventory")
                .replace("__ENTRY_CLASS__", "InventoryPlugin");
        Path packageFile = Files.createTempFile("companyware-starter-", ".cwp");
        try {
            try (ZipOutputStream archive = new ZipOutputStream(Files.newOutputStream(packageFile))) {
                archive.putNextEntry(new ZipEntry("plugin.yaml"));
                archive.write(descriptor.getBytes(StandardCharsets.UTF_8));
                archive.closeEntry();
                archive.putNextEntry(new ZipEntry("plugin.jar"));
                archive.write(new byte[] { 1 });
                archive.closeEntry();
            }
            PluginPackageInspection inspection = new PluginPackageInspector().inspect(packageFile);
            assertTrue("example.inventory".equals(inspection.getDescriptor().getId().value()));
            assertTrue(inspection.getEntries().size() == 2);
        } finally {
            Files.deleteIfExists(packageFile);
        }
    }
}
