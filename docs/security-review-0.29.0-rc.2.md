# Dependency and security review — 0.29.0-rc.2

This review applies only to the unpublished Companyware Desktop candidate
`0.29.0-rc.2`. Companyware Web has an independent dependency and security gate.

## Inherited reviewed baseline

- Java compatibility remains Java 8.
- Spring Boot `2.7.18`, Spring Framework `5.3.39`, Spring Security `5.8.16`,
  Hibernate `5.6.15.Final`, JUnit `4.13.2`, FlatLaf `3.6.2`, PDFBox `2.0.37`,
  SnakeYAML `2.7` and Maven Artifact `3.9.9` retain the reviewed RC1 baseline.
- Removed legacy coordinates and bundled runtime dependencies remain prohibited by
  `tools/verify-dependencies.ps1`; the exact resolved inventory is included in
  every release archive and bound into `release-evidence.json`.

## RC2-specific review

- The standalone plugin starter depends only on the version-matched public SDK
  classifier and creates source, tests, descriptor, documentation and package
  assembly without installing or activating generated code.
- Generated eligible source contains the accepted MIT/SPDX header and the starter
  includes the repository license.
- Navigation changes are presentation-state corrections and do not change
  authentication, authorization, persistence, plugin trust or migration contracts.
- The framework gate now rejects a mismatch between the Maven artifact version and
  the runtime-visible `companyware.version`.

## Remaining external gates

The deterministic inventory is not a live CVE feed. Immediately before public
release, review GitHub dependency/Dependabot findings for the exact commit; any
unresolved critical or high finding blocks publication. The packaged Windows
start/logout/shutdown cycle, plugin-JAR replacement, intended SMTP checks and a
backup/restore rehearsal with disposable operator-style data also remain required.
