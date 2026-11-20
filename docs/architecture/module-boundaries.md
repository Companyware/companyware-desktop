# Module and package boundaries

CW-D-001 establishes logical module boundaries inside the legacy single Maven
artifact. Physical Maven modules may follow after the public API has stabilized;
the dependency rules already apply to packages.

## Dependency direction

```text
plugins (presentation/infrastructure adapters)
                |
                v
   plugin application ports
                |
                v
          platform.api
                ^
                |
platform kernel + plugin runtime
```

The composition root may construct kernel/runtime implementations. It must not
import or cast to a concrete plugin implementation. Plugins implement and consume
public platform contracts. The platform API must remain independent of `core`,
`models`, `pluginmanager` implementations and `plugins`.

## Logical modules

| Logical module | Current package/path | Responsibility |
| --- | --- | --- |
| Bootstrap | `Companyware.java` | Process startup, shutdown and composition |
| Platform API | `platform.api.*` and transitional `pluginmanager.plugininterfaces.*` | Contracts visible to plugins and bootstrap |
| Kernel/runtime | `core.*`, `pluginmanager.*` excluding `plugininterfaces` | Lifecycle coordination and plugin runtime implementation |
| Persistence infrastructure | transitional `core.HibernateUtils`, `models.*` | Database adapters and persistence implementation |
| Desktop shell | `plugins.core.frame`, `plugins.core.menu` | Swing shell implementations behind platform contracts |
| Platform plugins | remaining `plugins.core.*` | Login, settings, users and plugin administration |
| Business plugins | `plugins.community.<plugin-id>.*` | Plugin-owned use cases, model, persistence and presentation |

`models.*` and cross-plugin imports are recorded legacy dependencies. They are
not public API and must not be expanded. The v1 SDK is defined under
`platform.api.*`; CW-D-006 and CW-D-008 migrate UI and business plugins away
from the remaining legacy imports.

## Layer convention within a plugin

- `domain`: business rules and domain objects; no Swing, database or kernel code.
- `application`: use cases and ports; depends on domain and public platform API.
- `infrastructure`: persistence, files and remote adapters; implements application ports.
- `presentation`: Swing controllers/views; invokes application use cases.

The maintained community plugins follow this boundary. Their Swing
views depend on application interfaces and immutable DTOs only. JDBC and
`PluginDatabase` calls live in `infrastructure` adapters assembled by the plugin
entry point. A later remote adapter can implement the same interface after a
versioned transport contract exists; plugin discovery and Swing views do not
change.

Existing Core `model`, `controller` and `view` packages are transitional and are
migrated by use case rather than renamed mechanically.

## Mechanical enforcement

`ArchitectureBoundariesTest` fails when the bootstrap, `core`, or plugin runtime
imports `plugins.*`, or when `platform.api` imports implementation packages. This
test also rejects JDBC/persistence imports in community presentation and Swing,
JDBC or persistence imports in community application packages.

## Plugin lifecycle

The kernel validates every descriptor before creating an entry-point instance.
It resolves minimum versions and cycles first, then starts plugins in dependency,
priority and stable-ID order. A failed plugin is rolled back without stopping
independent plugins; dependants are marked failed without executing their code.
Shutdown runs in reverse start order and closes SDK resources and owned loaders.
Installed descriptors are resolved at startup, while only enabled plugins start
initially. A newly installed trusted-local package is added to the same graph and
resolved without replacing already running runtimes. A recognized plugin can then start and stop through the same
lifecycle engine at runtime. Dependency guards reject unsafe starts and stops;
failed activation keeps the persisted and visible state unchanged.

Built-in plugins share the process loader. Each external package runtime owns one
dedicated closeable loader for its `plugin.jar`; resolution does not create it,
and stop or failed-start rollback closes it after plugin and context cleanup.
An inactive external definition may be replaced only by a higher version with the
same identity. Package swap, graph resolution and persisted metadata share a
rollback boundary. Removal is restricted to inactive external leaf definitions;
installed dependants and all built-in plugins are protected.

This complexity stays inside `pluginmanager.lifecycle`. An SDK plugin only needs
`initialize(PluginContext)`; cleanup hooks and dependency declarations are
optional.

The invoice plugin applies the same separation end to end: `domain` owns
validation, cent rounding and grouped tax totals; `application` owns the
transport-neutral invoice use cases and immutable DTOs; `infrastructure` owns JDBC
snapshots and the atomic number-series update; `view` owns the Swing overview and
draft editor. Presentation imports neither JDBC nor `PluginDatabase`. Finalization
changes the draft and its yearly sequence in one platform-owned transaction, so
failed finalization cannot consume a number.

Document rendering follows the same direction. `InvoiceDocumentService` is an
application port; the JDBC template store, controlled XHTML substitution and
OpenHTMLtoPDF adapter live in `infrastructure`. The Swing template editor, preview
and save dialog consume only that port. Templates control layout but cannot supply
invoice amounts, numbers or unescaped markup from business data.
