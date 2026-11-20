package core.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import platform.api.security.AuditEvent;
import platform.api.security.AuditSink;

public final class LoggingAuditSink implements AuditSink {
    private static final Log LOG = LogFactory.getLog("companyware.audit");
    @Override public void record(AuditEvent event) {
        LOG.info("audit occurredAt=" + event.occurredAt() + " action=" + safe(event.action())
                + " actor=" + safe(event.actor()) + " outcome=" + safe(event.outcome()));
    }
    private static String safe(String value) { return value.replace('\r', '_').replace('\n', '_'); }
}
