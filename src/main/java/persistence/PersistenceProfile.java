package persistence;

/** Supported Derby deployment modes. */
public enum PersistenceProfile {
    EMBEDDED("embedded"), CLIENT_SERVER("client-server");

    private final String configurationValue;
    PersistenceProfile(String configurationValue) { this.configurationValue = configurationValue; }
    public String configurationValue() { return configurationValue; }

    public static PersistenceProfile parse(String value) {
        for (PersistenceProfile profile : values()) {
            if (profile.configurationValue.equalsIgnoreCase(value)) return profile;
        }
        throw new IllegalArgumentException("Unsupported persistence profile: " + value);
    }
}
