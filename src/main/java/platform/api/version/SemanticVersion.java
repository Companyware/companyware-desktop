package platform.api.version;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable semantic version restricted to numeric major/minor/patch values. */
public final class SemanticVersion implements Comparable<SemanticVersion> {
    private static final Pattern FORMAT = Pattern.compile("^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$");

    private final int major;
    private final int minor;
    private final int patch;

    private SemanticVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SemanticVersion parse(String value) {
        Matcher matcher = FORMAT.matcher(Objects.requireNonNull(value, "version"));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid semantic version: " + value);
        }
        return new SemanticVersion(Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getPatch() { return patch; }

    public boolean isApiCompatibleWith(SemanticVersion required) {
        return major == required.major && compareTo(required) >= 0;
    }

    @Override public int compareTo(SemanticVersion other) {
        int result = Integer.compare(major, other.major);
        if (result == 0) result = Integer.compare(minor, other.minor);
        if (result == 0) result = Integer.compare(patch, other.patch);
        return result;
    }

    @Override public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof SemanticVersion)) return false;
        SemanticVersion other = (SemanticVersion) value;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override public int hashCode() { return Objects.hash(major, minor, patch); }
    @Override public String toString() { return major + "." + minor + "." + patch; }
}
