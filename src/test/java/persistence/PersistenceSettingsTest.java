package persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;
import org.junit.After;
import org.junit.Test;

public class PersistenceSettingsTest {
    @After public void clearOverrides() {
        System.clearProperty("companyware.persistence.profile");
        System.clearProperty("companyware.persistence.host");
        System.clearProperty("companyware.persistence.port");
        System.clearProperty("companyware.persistence.database");
        System.clearProperty("companyware.persistence.embedded.path");
    }

    @Test public void embeddedIsTheSimpleDefaultAndUsesNoNetworkUrl() {
        PersistenceSettings settings = PersistenceSettings.from(new Properties());
        assertEquals(PersistenceProfile.EMBEDDED, settings.getProfile());
        assertEquals("org.apache.derby.jdbc.EmbeddedDriver", settings.getDriverClass());
        assertTrue(settings.getJdbcUrl().startsWith("jdbc:derby:data/companyware"));
        assertTrue(!settings.getJdbcUrl().contains("//"));
    }

    @Test public void clientServerUsesExternalConfigOnly() {
        System.setProperty("companyware.persistence.profile", "client-server");
        System.setProperty("companyware.persistence.host", "db.internal");
        System.setProperty("companyware.persistence.port", "1621");
        System.setProperty("companyware.persistence.database", "companyware-prod");
        PersistenceSettings settings = PersistenceSettings.from(new Properties());
        assertEquals(PersistenceProfile.CLIENT_SERVER, settings.getProfile());
        assertEquals("org.apache.derby.jdbc.ClientDriver", settings.getDriverClass());
        assertEquals("jdbc:derby://db.internal:1621/companyware-prod;create=true", settings.getJdbcUrl());
    }
}
