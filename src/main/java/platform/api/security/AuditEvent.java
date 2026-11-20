package platform.api.security;

import java.time.Instant;
import java.util.Objects;

public final class AuditEvent {
    private final Instant occurredAt;
    private final String action;
    private final String actor;
    private final String outcome;

    public AuditEvent(String action, String actor, String outcome) {
        this.occurredAt = Instant.now();
        this.action = Objects.requireNonNull(action, "action");
        this.actor = actor == null || actor.trim().isEmpty() ? "anonymous" : actor.trim();
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }
    public Instant occurredAt() { return occurredAt; }
    public String action() { return action; }
    public String actor() { return actor; }
    public String outcome() { return outcome; }
}
