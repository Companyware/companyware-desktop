package core.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class StructuredLogTest {
    @Test public void producesStableSingleLineEscapedFields() {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("state", "STARTED");
        fields.put("message", "line one\nline two");
        assertEquals("event=plugin.health message=\"line one\\nline two\" state=STARTED",
                StructuredLog.event("plugin.health", fields));
    }

    @Test public void startupInventoryDoesNotContainDatabaseCredentials() {
        System.setProperty("companyware.persistence.profile", "client-server");
        System.setProperty("companyware.persistence.password", "must-not-appear");
        try {
            String output = StructuredLog.event("application.started",
                    StartupDiagnostics.capture(persistence.PersistenceSettings.load(), 3, 1));
            assertFalse(output.contains("must-not-appear"));
            assertFalse(output.toLowerCase().contains("password"));
        } finally {
            System.clearProperty("companyware.persistence.profile");
            System.clearProperty("companyware.persistence.password");
        }
    }
}
