package persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.junit.Test;

public class JdbcResourcesTest {
    @Test public void returnsConnectionWhenWorkAndCleanupFail() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        Connection connection = connection(true, true);
        ConnectionProvider provider = provider(connection, closed, true);
        Exception primary = new Exception("work");

        try {
            JdbcResources.withConnection(provider, value -> { throw primary; }, true);
            fail("Expected work failure");
        } catch (Exception failure) {
            assertSame(primary, failure);
            assertEquals(3, failure.getSuppressed().length);
        }
        assertTrue(closed.get());
    }

    @Test public void returnsConnectionBeforeReportingResetFailure() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        Connection connection = connection(false, true);
        ConnectionProvider provider = provider(connection, closed, false);

        try {
            JdbcResources.withConnection(provider, value -> "done", true);
            fail("Expected reset failure");
        } catch (Exception failure) {
            assertEquals("reset", failure.getMessage());
        }
        assertTrue(closed.get());
    }

    private static Connection connection(final boolean rollbackFails, final boolean resetFails) {
        return (Connection) Proxy.newProxyInstance(JdbcResourcesTest.class.getClassLoader(),
                new Class<?>[] { Connection.class }, (proxy, method, args) -> {
                    if ("rollback".equals(method.getName()) && rollbackFails) throw new SQLException("rollback");
                    if ("setAutoCommit".equals(method.getName()) && Boolean.TRUE.equals(args[0]) && resetFails)
                        throw new SQLException("reset");
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type == int.class) return 0;
                    if (type == long.class) return 0L;
                    return null;
                });
    }

    private static ConnectionProvider provider(final Connection connection,
            final AtomicBoolean closed, final boolean closeFails) {
        return (ConnectionProvider) Proxy.newProxyInstance(JdbcResourcesTest.class.getClassLoader(),
                new Class<?>[] { ConnectionProvider.class }, (proxy, method, args) -> {
                    if ("getConnection".equals(method.getName())) return connection;
                    if ("closeConnection".equals(method.getName())) {
                        closed.set(true);
                        if (closeFails) throw new SQLException("close");
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }
}
