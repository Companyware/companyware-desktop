## Summary

<!-- What changes, for whom, and what is explicitly out of scope? -->

## Desktop boundary

- [ ] This change belongs to Companyware Desktop.
- [ ] It does not claim to release Companyware Web or share Web plugin/runtime code.
- [ ] Public SDK/API compatibility and migrations are documented where affected.

## Verification

- [ ] `mvnw clean verify`
- [ ] `tools/verify-framework-release.ps1`
- [ ] Clean-clone release gate when release-relevant
- [ ] Manual runtime checks listed below

Actual evidence and unverified checks:

## Security, data and recovery

- [ ] No secrets, production data or generated local databases are included.
- [ ] Backup, upgrade and rollback effects are documented.
- [ ] User, developer and operator documentation is current.
