# GitHub repository migration

The historical public repository `Companyware/companyware` is not the target for
new releases. It contains an obsolete Desktop snapshot and must not receive the
current development history by accident.

## New repositories

- `Companyware/companyware-desktop` — Java/Swing application and Desktop plugin SDK
- `Companyware/companyware-web` — separate Symfony application and Web plugin SDK

`companyware-public` is not part of this GitHub repository migration.

## Safe sequence

1. Create both new GitHub repositories empty: no generated README, license or
   `.gitignore` commit.
2. Complete the independent Desktop and Web release gates in their authoritative
   Gitolite repositories.
3. Add each new GitHub remote only to its matching Source checkout and verify the
   URL before any push.
4. Push only the reviewed candidate commit to the new repository's `main` branch.
5. Verify CI and public documentation before creating a preview tag or release.
6. In a separately approved change, replace the old repository README with a
   migration notice that links to `companyware-desktop`, then archive the old
   repository. Do not delete its history or silently repurpose it as Companyware Web.

## Remote safety

The old local Desktop remote currently named `github` must be renamed to
`github-legacy` before adding the new `github` remote. The Web checkout receives
only the `companyware-web` remote. Remote changes and pushes require explicit
approval after the repositories exist.
