package persistence;

import java.sql.Connection;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import platform.api.persistence.SqlWork;

/** Ensures provider-owned JDBC connections are returned even when cleanup itself fails. */
final class JdbcResources {
    private JdbcResources() {}

    static <T> T withConnection(ConnectionProvider provider, SqlWork<T> work,
            boolean transactional) throws Exception {
        Connection connection = provider.getConnection();
        Exception failure = null;
        try {
            if (transactional) connection.setAutoCommit(false);
            T result = work.execute(connection);
            if (transactional) connection.commit();
            return result;
        } catch (Exception exception) {
            failure = exception;
            if (transactional) {
                try { connection.rollback(); }
                catch (Exception rollbackFailure) { exception.addSuppressed(rollbackFailure); }
            }
            throw exception;
        } finally {
            Exception resetFailure = null;
            if (transactional) {
                try { connection.setAutoCommit(true); }
                catch (Exception exception) {
                    resetFailure = exception;
                    if (failure != null) failure.addSuppressed(exception);
                }
            }
            try { provider.closeConnection(connection); }
            catch (Exception closeFailure) {
                if (failure != null) failure.addSuppressed(closeFailure);
                else if (resetFailure != null) resetFailure.addSuppressed(closeFailure);
                else throw closeFailure;
            }
            if (failure == null && resetFailure != null) throw resetFailure;
        }
    }
}
