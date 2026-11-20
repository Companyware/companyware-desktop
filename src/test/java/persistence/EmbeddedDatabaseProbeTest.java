package persistence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import org.junit.Test;

public class EmbeddedDatabaseProbeTest {
    @Test public void recognizesDerbysNestedAlreadyBootedFailure() {
        SQLException outer = new SQLException("Failed to start database");
        outer.setNextException(new SQLException(
                "Another instance of Derby may have already booted the database C:/data/companyware."));
        assertTrue(EmbeddedDatabaseProbe.isAlreadyBooted(outer));
    }

    @Test public void doesNotHideOtherDatabaseFailures() {
        assertFalse(EmbeddedDatabaseProbe.isAlreadyBooted(new SQLException("Invalid credentials")));
    }
}
