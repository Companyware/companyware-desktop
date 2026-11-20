package platform.api.event;

/** Publishes typed events. Dispatch semantics are implemented in CW-D-003. */
public interface EventPublisher {
    <T> void publish(EventKey<T> event, T payload);
}
