# Contributing to Companyware Desktop

Thank you for helping improve Companyware Desktop. This repository contains the
Java/Swing application only. Companyware Web is developed and released separately.

## Before starting

1. Read `README.md`, `docs/README.md` and `docs/developer-guide.md`.
2. Discuss substantial public contracts, migrations or security changes before
   implementation.
3. Use a JDK 8, not a JRE, and keep `JAVA_HOME` pointed at that JDK.
4. Never add production databases, credentials, invoices, customer documents,
   IDE caches or generated release artifacts to Git.

## Build and test

On Windows:

```powershell
.\mvnw.cmd clean verify
.\tools\verify-framework-release.ps1
```

The framework gate checks the public-source policy and documentation, runs the
complete Maven suite and creates reproducible artifacts plus checksums. Before a
release candidate, also run the clean-clone gate:

```powershell
.\tools\verify-clean-clone.ps1
```

Do not describe a check as passed if it was skipped or could not run.

## Source and plugin boundaries

- `platform.api.*` is the public Desktop plugin SDK.
- Core and implementation packages are not extension contracts.
- Plugins own their use cases, persistence adapters, migrations and UI
  contributions.
- Desktop `.cwp` packages are not Symfony/Web plugins.
- Cross-product communication requires a reviewed, versioned API contract.

New eligible source files use the Companyware MIT/SPDX header described in
`docs/framework-release.md`. Do not add headers to third-party or generated code
without reviewing its provenance.

## Pull requests

Keep changes focused. Update user, developer and operating documentation whenever
behavior or a public contract changes. A pull request must state:

- user-visible behavior and exclusions;
- affected public contracts, migrations and compatibility;
- tests and builds actually executed;
- security, data, backup and recovery impact;
- checks that remain unverified.

Use disposable data in tests and screenshots. Maintainers may require a clean
rebase and repeatable release evidence before merge.
