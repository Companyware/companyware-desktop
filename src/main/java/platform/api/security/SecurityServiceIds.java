package platform.api.security;

import platform.api.service.ServiceKey;

public final class SecurityServiceIds {
    public static final ServiceKey<AuthenticationService> AUTHENTICATION = ServiceKey.of("platform.authentication", AuthenticationService.class);
    public static final ServiceKey<AuthorizationService> AUTHORIZATION = ServiceKey.of("platform.authorization", AuthorizationService.class);
    public static final ServiceKey<AuditSink> AUDIT = ServiceKey.of("platform.audit", AuditSink.class);
    public static final ServiceKey<SecretProvider> SECRETS = ServiceKey.of("platform.secrets", SecretProvider.class);
    public static final ServiceKey<MutableSecretProvider> MUTABLE_SECRETS = ServiceKey.of("platform.mutable-secrets", MutableSecretProvider.class);
    public static final ServiceKey<AccountRecoveryService> RECOVERY_KEYS = ServiceKey.of("platform.recovery-keys", AccountRecoveryService.class);
    private SecurityServiceIds() {}
}
