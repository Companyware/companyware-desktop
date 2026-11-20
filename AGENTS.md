# Companyware Desktop agent instructions

This repository is self-contained. Do not require a private Knowledge repository,
an internal Git server or files from another checkout to build, test, run or
release Companyware Desktop.

Before project work:

1. Read `README.md` and `docs/README.md`.
2. Preserve all pre-existing local changes. If the working tree is not clean,
   report the changes and stop before pulling, committing or releasing.
3. Pull the current branch only with `git pull --ff-only` when it has an upstream.
4. Use a 64-bit JDK 8 for the supported build and release gates.
5. Keep test data below `target` or another disposable directory; never use or
   modify production data.

For implementation changes, update the affected user, developer and operating
documentation. Run the relevant tests and never report an unexecuted check as
passed.

For a release, follow `docs/release-howto.md` or
`docs/codex-release-howto.md`. Do not create or move a tag, publish a GitHub
release, upload artifacts or bypass a failed gate without the maintainer's
explicit approval for the exact commit and version. JAR, ZIP and EXE files do not
belong in Git history.
