package persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Detects an embedded Derby database already booted by an older application process. */
public final class EmbeddedDatabaseProbe {
    private EmbeddedDatabaseProbe() { }

    public static void verifyAvailable(PersistenceSettings settings) throws Exception {
        if (settings.getProfile() != PersistenceProfile.EMBEDDED) return;
        Class.forName(settings.getDriverClass());
        try (Connection ignored = DriverManager.getConnection(settings.getJdbcUrl(),
                settings.getUsername(), settings.getPassword())) {
            // A successful probe boots Derby in this JVM; Hibernate reuses that engine.
        } catch (SQLException failure) {
            if (isAlreadyBooted(failure)) {
                throw new EmbeddedDatabaseInUseException(settings.getEmbeddedPath().toAbsolutePath().normalize());
            }
            throw failure;
        }
    }

    static boolean isAlreadyBooted(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains("Another instance of Derby may have already booted")) return true;
            if (current instanceof SQLException) {
                SQLException sql = (SQLException) current;
                for (SQLException next = sql.getNextException(); next != null; next = next.getNextException()) {
                    String nextMessage = next.getMessage();
                    if (nextMessage != null && nextMessage.contains("Another instance of Derby may have already booted")) return true;
                }
            }
        }
        return false;
    }
}
