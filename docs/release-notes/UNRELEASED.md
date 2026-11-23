# Companyware Desktop — unreleased development preview

This document is a preparation record, not a published release announcement.
The published Development Preview is documented in `0.29.0.md`; the earlier
preparation records remain in `0.29.0-rc.1.md` and `0.29.0-rc.2.md`. A future publication date,
signed tag and public artifact links are filled only after every release gate has
passed and publication is explicitly approved.

## Scope

- Java 8/Swing Desktop application with its own `.cwp` plugin runtime.
- Public Desktop Plugin SDK v1 under `platform.api.*`.
- AP-02 standalone Java-8 plugin starter with a version-bound public SDK
  classifier, safe project generation, tests and reproducible `.cwp` packaging;
  it does not install or activate generated plugins.
- Embedded Apache Derby for private use and external Derby Network Server profile
  for controlled multi-client operation.
- Trusted-local plugin inspection, installation, activation, update and uninstall.
- Separate light/dark theme providers, localization, identity, permissions,
  recovery and audit foundations.
- Reference plugins and the current invoice workflow.

Companyware Web is a separate Symfony application. It is not included in this
artifact and has its own release evidence. Interoperability requires separately
versioned API packages.

## Required evidence before publication

- [ ] Exact release version and immutable commit selected.
- [ ] Full JDK-backed `tools/verify-framework-release.ps1` passed.
- [ ] Exact-commit `tools/verify-clean-clone.ps1` passed twice with matching
      `Companyware.jar` and ZIP hashes.
- [ ] Dependency/security findings reviewed with no unresolved critical issue.
- [ ] License/provenance review completed.
- [ ] Embedded and Network Server migrations, login/recovery, plugin lifecycle,
      shutdown/file-release and backup/restore runtime checks completed.
- [ ] Upgrade and rollback rehearsed against disposable data.
- [ ] User, developer, operator, security and support documentation aligned.
- [ ] GitHub and website identify the same commit, version and checksums.
- [ ] Signed semantic tag and release explicitly approved.

## Known non-release evidence

The policy-only gate verifies source headers and required documentation but does
not build or test the application. It must never be used as publication evidence.
