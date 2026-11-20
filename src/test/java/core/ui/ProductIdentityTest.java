// Companyware
// Copyright (c) 2026 Companyware contributors
//
// SPDX-License-Identifier: MIT
// Licensed under the MIT License; see LICENSE in the project root.
// This file is part of Companyware.
package core.ui;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ProductIdentityTest {
    @Test
    public void visibleSurfacesIdentifyCompanywareDesktop() throws Exception {
        assertContains("src/main/resources/messages.properties", "program.name = Companyware Desktop");
        assertContains("src/main/java/plugins/core/frame/messages_de.properties", "program.name = Companyware Desktop");
        assertContains("src/main/java/plugins/core/login/messages_de.properties", "Companyware Desktop - Anmeldung");
        assertContains("src/main/java/core/StartProgressBar.java", ">Desktop</span>");
        assertContains("src/main/java/plugins/core/frame/view/Frame.java", ">Desktop</span>");
        assertContains("src/main/java/plugins/core/login/view/LoginView.java", ">Desktop</span>");
        assertContains("src/main/java/plugins/core/info/view/InfoView.java", "Companyware Desktop</td>");
        assertContains("pom.xml", "<name>Companyware Desktop</name>");
        assertContains("pom.xml", "<productName>Companyware Desktop</productName>");
        assertContains("launch4j.xml", "<errTitle>Companyware Desktop</errTitle>");
    }

    @Test
    public void technicalIdentityRemainsStable() throws Exception {
        assertContains("pom.xml", "<artifactId>Companyware</artifactId>");
        assertContains("src/main/java/platform/api/ApiVersion.java", "Companyware Desktop plugin API");
        assertContains("src/main/java/plugins/core/frame/Frame.yaml", "id: companyware.core.frame");
    }

    private static void assertContains(String path, String expected) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        assertTrue(path + " must contain " + expected, content.contains(expected));
    }
}
