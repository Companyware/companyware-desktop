# Dependency and security review — 0.29.0-rc.1

This review applies only to the unpublished Companyware Desktop preview
`0.29.0-rc.1`. Companyware Web has an independent dependency and security gate.

## Reviewed baseline

- Java compatibility remains Java 8.
- Spring Boot is updated to the final public Java-8 line `2.7.18`; its framework
  dependencies are overridden to the latest public Java-8 maintenance artifacts
  available from Maven Central: Spring Framework `5.3.39` and Spring Security
  `5.8.16`.
- Hibernate is updated to `5.6.15.Final`, JUnit to `4.13.2`, FlatLaf to `3.6.2`,
  PDFBox to `2.0.37`, SnakeYAML to `2.7` and Maven Artifact to `3.9.9`.
- The unused direct Log4j 1.x, Commons Logging, Commons IO, fluent-hibernate and
  bundled Oracle JRE dependencies are removed.
- Logback is pinned to `1.2.13` and Log4j 2 components to `2.17.2` while Java 8
  remains supported.

`tools/verify-dependencies.ps1` resolves the complete Maven tree, writes the
exact inventory into every release candidate and rejects the removed legacy
coordinates and any regression from the reviewed Spring Boot/Hibernate baseline.
The inventory SHA-256 is recorded in `release-evidence.json`.

## Security gates

- All identity, password-policy, recovery, migration, plugin-package and
  lifecycle tests are part of the Maven gate.
- `tools/verify-publication-safety.ps1` rejects tracked credentials, runtime
  databases, logs, generated packages and writable GitHub workflow permissions.
- The public package contains the MIT license, private vulnerability reporting
  instructions, support scope, exact commit and checksums.
- The online Derby backup/restore path is exercised against a disposable database
  by `DerbyBackupRestoreIntegrationTest`.

## Boundaries and residual review

The dependency inventory is a deterministic local policy and provenance gate; it
is not a live external CVE feed. Immediately before publication, GitHub dependency
review/Dependabot and the repository security-advisory view must be checked for
new findings against the exact commit. A critical or high finding blocks
publication. Real SMTP delivery, a packaged Windows launch/shutdown cycle and a
restore rehearsal with operator-owned data remain environment-specific external
acceptance checks and are not claimed by the local automated suite.
