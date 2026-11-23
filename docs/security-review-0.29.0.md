# Dependency and security review — 0.29.0

This review applies to the Companyware Desktop 0.29.0 Development Preview.
Companyware Web has an independent dependency and security gate.

## Reviewed baseline

- Java compatibility remains Java 8; the installer packages the Temurin Java 8
  runtime selected by `actions/setup-java` on the pinned Windows runner.
- Spring Boot `2.7.18`, Spring Framework `5.3.39`, Spring Security `5.8.16`,
  Hibernate `5.6.15.Final`, JUnit `4.13.2`, FlatLaf `3.6.2`, PDFBox `2.0.37`,
  SnakeYAML `2.7` and Maven Artifact `3.9.9` retain the reviewed RC2 baseline.
- `tools/verify-dependencies.ps1` rejects prohibited legacy coordinates and binds
  the exact resolved inventory into the release evidence.
- The write-enabled GitHub workflow runs only for an explicit `v*` tag and cannot
  be triggered by a pull request. Tag and `pom.xml` version must match exactly.
- Installer files and user data use separate per-user directories. Program
  upgrades do not intentionally delete the embedded database or installed
  plugins.

## Known limitations

- The installer is not Authenticode-signed; origin and SHA-256 verification are
  mandatory for this Development Preview.
- A successful build is not evidence that all production operator scenarios are
  approved. Backup/restore, intended SMTP behavior and upgrade testing remain
  deployment responsibilities.
- Plugin code remains trusted in-process code. Third-party distribution still
  requires publisher signatures and the remaining trust gates.
