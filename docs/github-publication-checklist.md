# GitHub publication checklist — Companyware Desktop

This checklist prepares a release; it does not authorize a GitHub push, tag or
release. Gitolite `develop` remains authoritative until explicit approval.

## Exact candidate

- Product: **Companyware Desktop**
- Candidate: `0.29.0-rc.2`
- Commit: fill from `git rev-parse HEAD` only after the preparation commit is
  reviewed and selected
- Public target: `Companyware/companyware-desktop` branch `main`
- Companyware Web: explicitly excluded

## Repository-owned gates

- [ ] Working tree is clean and synchronized with `git pull --ff-only`.
- [ ] `pom.xml`, runtime `companyware.version`, release notes and SDK starter use
      the same candidate version.
- [ ] `tools/verify-framework-release.ps1` passes with a JDK.
- [ ] `tools/verify-clean-clone.ps1` produces byte-identical JAR, ZIP, dependency
      inventory and release evidence from two clean clones.
- [ ] README, user/developer/operator docs, LICENSE, SECURITY, SUPPORT,
      CONTRIBUTING and CHANGELOG match the exact candidate.
- [ ] No runtime data, credentials, logs, generated packages or historical crash
      files are tracked.

## External gates

- [ ] GitHub dependency/Dependabot and security-advisory views show no unresolved
      critical or high finding for the exact commit.
- [ ] Packaged Windows start, login, logout, shutdown and file-handle release pass.
- [ ] Intended SMTP configuration and recovery/mail behavior are verified with
      disposable data and no secret in logs.
- [ ] Backup and restore are rehearsed with disposable operator-style data.
- [ ] Any later external download reference names the same version, commit and
      SHA-256 values; `companyware-public` is not a GitHub publication repository.
- [ ] Public repository settings enable private vulnerability reporting and
      protect immutable release tags.

## Approved publication sequence

1. Record the exact verified commit and hashes from `release-evidence.json`.
2. Obtain explicit publication approval.
3. Update GitHub `main` to that exact reviewed commit without rewriting history.
4. Create an annotated tag `v0.29.0-rc.2` on the same commit.
5. Mark the GitHub release as a **pre-release** and upload only the verified
   Windows ZIP, checksum and evidence files. The ZIP is the quick-start download
   and must contain the exact verified `Companyware.jar`, `Companyware.exe`,
   `start-companyware.cmd`, matching documentation and JAR/EXE checksums. Do not commit JAR, ZIP or EXE binaries
   to the Git source history. Publish a native EXE only after its separate Windows
   packaging and start/stop gate passes.
6. Re-read the public README and download links, then record the GitHub commit,
   tag and release URL in Knowledge.
