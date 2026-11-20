# Companyware Desktop

Companyware is the product family for two independently released applications:

- **Companyware Desktop** is the Java 8/Swing application in this repository. It
  runs locally, supports installable `.cwp` plugins and uses Apache Derby.
- **Companyware Web** is the separate Symfony application. It has its own runtime,
  plugin format, build, tests and release lifecycle.

Desktop and Web may cooperate through explicitly versioned HTTP/API contracts.
They do not share plugin binaries, database tables or release status. This
repository never claims that a Desktop release also releases the Web application.

> **Development preview:** the current branch is framework-development source,
> not a supported stable release or an installer download. Use only immutable
> tagged artifacts whose checksums and release notes have passed the documented
> release gate.

## Download and quick start

Executable binaries are not committed to the Git source history. Each approved
GitHub pre-release provides a versioned Windows ZIP as a release asset after its
release gates pass. The ZIP contains `Companyware.jar`,
`start-companyware.cmd`, the matching documentation and the JAR checksum. A user
with a supported Java runtime can unpack the ZIP and start the command file
without compiling the source. A native EXE may be added only after its separate
Windows packaging and start/stop gate has passed.

Branches and untagged commits remain source-only Development Previews. Download
executable files only from an explicitly marked GitHub Release or Pre-release,
never from commits, issues or unofficial mirrors.

Developer setup, plugin authoring, persistence, security and testing are documented
in [the developer guide](docs/developer-guide.md).

The German operating instructions are available in the
[Companyware user guide](docs/benutzerhandbuch.md).

All documentation starts at the [documentation overview](docs/README.md). Every
work package updates the affected user, developer and operating documentation as
part of its completion criteria.

The project is licensed under the MIT License; see [LICENSE](LICENSE). The stable
public plugin boundary and framework publication policy are documented in
[docs/framework-release.md](docs/framework-release.md).

## Prerequisites

- A 64-bit JDK 8. A JRE is sufficient to run the application but not to build it.
- No separate database server is required for the default private/embedded profile. The optional company profile requires an externally operated Derby Network Server.
- Eclipse IDE for Java Developers Mars 2 with m2e is supported for the legacy workspace workflow. Use a 64-bit Eclipse with a 64-bit Java runtime.

No global Maven installation is required. The checked-in Maven Wrapper downloads the pinned Maven distribution and verifies its SHA-256 checksum.

## Build

On Windows:

```powershell
.\mvnw.cmd clean verify
```

On Linux or macOS:

```sh
./mvnw clean verify
```

The executable Spring Boot JAR is created as `target/Companyware.jar`. The optional legacy Windows packaging profile is invoked with `-Pwindows-exe`; it is not part of the baseline verification.

For a verified distributable archive, set `JAVA_HOME` to a JDK and run
`.\tools\verify-framework-release.ps1`. The complete clean-clone release gate and rollback
procedure are documented in [docs/release-process.md](docs/release-process.md).

## Eclipse import

1. Select **File → Import → Existing Maven Projects**.
2. Choose the repository root containing `pom.xml`.
3. Select a Java 8 JDK as the project JRE.
4. Run **Maven → Update Project** if dependencies are not resolved immediately.
5. Launch the existing `Companyware` Java application configuration or the `Companyware` main class.

The repository contains no machine-specific workspace path. The local libraries in `lib/` are referenced through repository-relative Eclipse classpath entries.

## Startup smoke test

1. Use the default embedded profile, or start the external Derby Network Server when testing the `client-server` profile.
2. Run `.\mvnw.cmd clean verify`.
3. Start `Companyware` from Eclipse or run `java -jar target/Companyware.jar`.
4. Confirm that the login flow completes and the main window renders the plugin manager.
5. Open the plugin manager and confirm that **Plugin installieren** accepts a `.cwp` file after an explicit local-trust confirmation.

## Derby profiles and migrations

The default `embedded` profile stores its database below `data/companyware` and never starts a network listener. Start with another location using:

```powershell
java -Dcompanyware.persistence.embedded.path=C:/Companyware/data/companyware -jar target/Companyware.jar
```

For several desktop clients, operate Derby Network Server independently and select the client profile:

```powershell
java -Dcompanyware.persistence.profile=client-server `
  -Dcompanyware.persistence.host=db.internal `
  -Dcompanyware.persistence.port=1527 `
  -Dcompanyware.persistence.database=companyware `
  -Dcompanyware.persistence.username=app `
  -Dcompanyware.persistence.password=change-me `
  -jar target/Companyware.jar
```

Core and plugin schema changes use the same ordered migration history in both profiles. Migration identity is `(plugin owner, version)`, applied SQL is checksummed, and each migration is committed or rolled back as one transaction. Applied migrations must never be edited; add a higher version instead.

## Plugin package format

A Companyware plugin package uses the `.cwp` extension and is a ZIP archive with these required root entries:

- `plugin.yaml`: the public SDK descriptor (including a compatible `requiredApiVersion`)
- `plugin.jar`: the plugin implementation

Additional libraries and resources may use subdirectories. The installer inspects the entire archive before extraction, never loads plugin code during inspection, enforces path and resource limits, stores the package SHA-256 checksum and installs through a staging directory. Failed validation leaves an existing installation untouched. Packages are currently governed by an explicit `trusted-local` policy; cryptographic signatures are required before accepting third-party distribution. Installed packages are stored below `plugins-installed` by default, configurable with `-Dcompanyware.plugins.directory=...`. Runtime activation of these external packages is intentionally deferred to a later work package.

## Simple UI contributions

An SDK plugin contributes Swing UI through its `PluginContext`; it does not import Companyware frame, menu or controller implementations:

```java
public void initialize(PluginContext context) {
    context.addView("orders", "Orders", 100, () -> new OrdersPanel());
    context.addNavigation("orders.navigation", "Orders", 100, "orders");
    context.addMenuItem("orders.refresh", "Refresh", 100, "orders", this::refresh);
    context.addSettingsPage("orders.settings", "Order settings", 100, () -> new SettingsPanel());
}
```

Contribution ownership, deterministic ordering and removal are automatic. Registry mutations and change notifications run on Swing's Event Dispatch Thread. Contributions can declare authentication or permission requirements through the public contribution specification when the convenience defaults are not sufficient.

Themes use the public `ThemeProvider`/`ThemeRegistry` contracts. One complete base theme is active at a time. Partial overlays apply in ascending priority and plugin-ID order; later non-null color/font values and non-negative spacing values win. The built-in Companyware Light theme is a protected fallback.

## Identity and security

Authentication, authorization, auditing and secret lookup are typed platform
services under `platform.api.security`. New users and the built-in administrator
must replace their initial password; the replacement must contain at least ten
characters. Roles and permissions belong to the authenticated identity. Plugins
declare namespace-safe privileges with `context.permission("orders.edit")` and use
them in UI access requirements instead of importing login or user implementations.

On a fresh Desktop database, the built-in administrator uses username
`companyware` and initial password `companyware`. These are first-login credentials,
not a permanent shared account: the login flow requires an immediate replacement
with a password of at least ten characters, and the known initial password cannot
be assigned again. Existing installations continue to use their already changed
credentials. Companyware Web does not use this Desktop account.

Secrets do not belong in source, `application.properties` or plugin descriptors.
The default provider reads `CW_SECRET_<NAME>` environment variables, with
`-Dcompanyware.secret.<name>=...` available for local JVM configuration. Login,
credential changes, local plugin installation and user administration emit audit
events without recording passwords or secret values.

## Database backup

Follow [docs/derby-backup.md](docs/derby-backup.md) before schema-affecting work or upgrades.
