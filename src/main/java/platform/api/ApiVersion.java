package platform.api;

import platform.api.version.SemanticVersion;

/** Version of the public Companyware Desktop plugin API. */
public final class ApiVersion {
    public static final SemanticVersion CURRENT = SemanticVersion.parse("1.0.0");

    private ApiVersion() { }
}
