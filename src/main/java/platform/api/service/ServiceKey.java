package platform.api.service;

import java.util.Objects;
/** Type-safe identity for one platform capability/service. */
public final class ServiceKey<T extends Service> {
    private final String id;
    private final Class<T> serviceType;

    private ServiceKey(String id, Class<T> serviceType) {
        if (id == null || !id.matches("^[a-z][a-z0-9]*(?:[.-][a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid service id: " + id);
        }
        this.id = id;
        this.serviceType = Objects.requireNonNull(serviceType, "serviceType");
    }

    public static <T extends Service> ServiceKey<T> of(String id, Class<T> serviceType) {
        return new ServiceKey<T>(id, serviceType);
    }

    public String getId() { return id; }
    public Class<T> getServiceType() { return serviceType; }
    @Override public int hashCode() { return Objects.hash(id, serviceType); }
    @Override public boolean equals(Object value) {
        if (!(value instanceof ServiceKey)) return false;
        ServiceKey<?> other = (ServiceKey<?>) value;
        return id.equals(other.id) && serviceType.equals(other.serviceType);
    }
    @Override public String toString() { return id + "<" + serviceType.getName() + ">"; }
}
