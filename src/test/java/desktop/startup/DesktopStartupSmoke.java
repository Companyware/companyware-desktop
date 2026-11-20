// Companyware
// Copyright (c) 2026 Companyware contributors
// SPDX-License-Identifier: MIT

package desktop.startup;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.hibernate.Session;
import org.hibernate.Version;
import org.springframework.boot.SpringBootVersion;

import core.HibernateUtils;
import pluginmanager.PluginDescriptorReader;

/** Standalone smoke probe that can be run with the same Java 8 runtime as Eclipse. */
public final class DesktopStartupSmoke {
    private DesktopStartupSmoke() { }

    public static void main(String[] args) throws Exception {
        require("2.7.18", SpringBootVersion.getVersion(), "Spring Boot");
        require("5.6.15.Final", Version.getVersionString(), "Hibernate");

        String descriptor = "id: smoke.plugin\n"
                + "name: Smoke Plugin\n"
                + "author: Companyware\n"
                + "version: 1.0.0\n"
                + "requiredApiVersion: 1.0.0\n"
                + "entryPoint: smoke.Plugin\n"
                + "capabilities: []\n";
        PluginDescriptorReader.read(
                new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)), "startup-smoke");

        try (Session session = HibernateUtils.getSessionFactory().openSession()) {
            if (!session.isOpen()) throw new IllegalStateException("Hibernate session did not open");
        } finally {
            HibernateUtils.shutdown();
        }
        System.out.println("Companyware Desktop startup smoke passed on " + System.getProperty("java.version"));
    }

    private static void require(String expected, String actual, String component) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(component + " mismatch: expected " + expected + ", got " + actual);
        }
    }
}
