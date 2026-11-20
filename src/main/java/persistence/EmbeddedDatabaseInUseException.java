package persistence;

import java.io.IOException;
import java.nio.file.Path;

/** Raised when another process already owns the selected embedded database. */
public final class EmbeddedDatabaseInUseException extends IOException {
    private static final long serialVersionUID = 1L;

    EmbeddedDatabaseInUseException(Path database) {
        super("Companyware is already using the embedded database: " + database);
    }
}
