package platform.api.persistence;

import java.sql.Connection;

/** JDBC operation executed with a platform-managed connection. */
@FunctionalInterface
public interface SqlWork<T> {
    T execute(Connection connection) throws Exception;
}
