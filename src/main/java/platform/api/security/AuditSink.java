package platform.api.security;

import platform.api.service.Service;

public interface AuditSink extends Service {
    void record(AuditEvent event);
}
