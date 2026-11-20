// Companyware
// Copyright (c) 2026 Companyware contributors
// SPDX-License-Identifier: MIT

package architecture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.hibernate.Version;
import org.junit.Test;
import org.springframework.boot.SpringBootVersion;
import org.yaml.snakeyaml.LoaderOptions;

/** Prevents Eclipse from silently mixing Maven dependencies with bundled legacy JARs. */
public class RuntimeBaselineTest {

    @Test
    public void runtimeUsesReviewedMavenBaseline() {
        assertEquals("2.7.18", SpringBootVersion.getVersion());
        assertEquals("5.6.15.Final", Version.getVersionString());
        assertNotNull(new LoaderOptions());
        assertFalse(codeSource(LoaderOptions.class).contains("/lib/"));
    }

    @Test
    public void eclipseClasspathUsesMavenAsOnlyDependencySource() throws Exception {
        File classpath = new File(".classpath");
        String xml = new String(Files.readAllBytes(classpath.toPath()), StandardCharsets.UTF_8);
        assertFalse(xml.contains("kind=\"lib\""));
        assertFalse(xml.contains("path=\"lib\""));
        assertFalse(xml.contains("excluding=\"main/java/|test/java/|main/resources/\""));
    }

    private static String codeSource(Class<?> type) {
        return type.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
    }
}
