package persistence;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.Test;

public class EmbeddedInstanceGuardTest {
    @Test public void blocksASecondOwnerAndReleasesOnClose() throws Exception {
        Path directory = Files.createTempDirectory("companyware-instance-guard-");
        Properties properties = new Properties();
        properties.setProperty("companyware.persistence.embedded.path", directory.resolve("database").toString());
        PersistenceSettings settings = PersistenceSettings.from(properties);

        EmbeddedInstanceGuard first = EmbeddedInstanceGuard.acquire(settings);
        try {
            try {
                EmbeddedInstanceGuard.acquire(settings);
                fail("Second embedded owner must be rejected");
            } catch (EmbeddedDatabaseInUseException expected) {
                assertNotNull(expected.getMessage());
            }
        } finally {
            first.close();
        }

        EmbeddedInstanceGuard next = EmbeddedInstanceGuard.acquire(settings);
        next.close();
    }

    @Test public void clientServerModeDoesNotTakeALocalLock() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("companyware.persistence.profile", "client-server");
        PersistenceSettings settings = PersistenceSettings.from(properties);
        EmbeddedInstanceGuard first = EmbeddedInstanceGuard.acquire(settings);
        EmbeddedInstanceGuard second = EmbeddedInstanceGuard.acquire(settings);
        first.close();
        second.close();
    }
}
