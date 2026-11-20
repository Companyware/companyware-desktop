# Companyware Desktop Plugin SDK v1

Plugins compile against `platform.api.*`. They must not import `core`, `models`,
`pluginmanager` or another plugin implementation. The compile-only example in
`src/test/java/sdkexample/ExamplePlugin.java` and the architecture tests enforce
that boundary.

## Descriptor

Every plugin provides `<EntryClass>.yaml` beside its entry class. The v1 schema
is `src/main/resources/plugin-descriptor.schema.json`. Required fields are:

- `id`: stable lowercase identity;
- `name` and `author`: display metadata;
- `version`: numeric semantic version;
- `requiredApiVersion`: minimum compatible SDK version with the same major;
- `entryPoint`: fully qualified entry class;
- `capabilities`: stable capability IDs.

Optional `dependencies` contain a plugin ID and minimum version. Dependencies
always start first. Otherwise `priority` and then the stable plugin ID determine
the order, so the result is identical on every machine.

Descriptors are parsed and validated before discovery persists plugin metadata.
YAML files are packaged as Maven resources.

## Public contracts

- `platform.api.plugin`: identity, descriptor, dependencies, entry point and context;
- `platform.api.service`: typed service keys plus capability IDs and registry;
- `platform.api.event`: typed, explicitly owned events;
- `platform.api.configuration`: plugin-scoped configuration;
- `platform.api.i18n`: plugin-scoped localization;
- `platform.api.ui`: Swing contributions and shell/login boundaries;
- `platform.api.theme`: semantic theme tokens and versioned providers.
- `platform.api.persistence`: plugin migrations and transaction-scoped JDBC work.

The legacy string registry and old plugin interfaces remain available only as a
compatibility bridge. New platform services use `ServiceKey<T>`; duplicate typed
keys are rejected instead of silently overwritten.

The platform owns dependency resolution, startup rollback and reverse-order
shutdown. A simple plugin implements only `initialize`. Plugins which own a
resource may register it with `context.closeOnStop(resource)` or override the
optional no-op `stop()` hook. Recognized plugins can be activated and deactivated
at runtime. Dependencies must already be active; an active provider cannot be
stopped while a started consumer depends on it. SDK registrations made through the
context are removed automatically during deactivation or failed startup.

`PluginContext.database()` is the only supported persistence gateway for new
plugins. Use `migrate(...)` for versioned schema changes and `read(...)` or
`write(...)` for short JDBC operations. The platform selects Embedded or Network
Server mode; plugin code must not select a driver or database URL.

Localized menu items are registered with `addLocalizedMenuItem(...)`. Their
labels follow a runtime language change automatically. See
[`reference-plugins.md`](reference-plugins.md) for complete runnable examples and
the `.cwp` packaging commands.

`addLocalizedView(...)` and `addLocalizedNavigation(...)` form an application
destination: the navigation contribution targets the stable view ID owned by the
same plugin. The shell renders it in the OutlookBar and creates the Swing view only
when selected. Runtime deactivation removes the destination and view together.

`addLocalizedNavigationGroup(...)` adds a localized OutlookBar group before
several consecutive, ordered destinations owned by the same plugin. The shell
shows those destinations with a visual indent only while the group is selected;
the child panel is placed before the next top-level OutlookBar item. Every child
remains a normal view-targeting navigation contribution.

The overloads `addNavigation(..., Icon)` and `addLocalizedNavigation(..., Icon)`
give a destination an optional Swing icon. Group headings remain text-only;
children are rendered as centered icon-and-label tiles. Prefer the public semantic
constants in `CompanywareIcons` (`OVERVIEW`, `CREATE`, `EDIT`, `DOCUMENT`, `IMAGE`,
`SEARCH`, `MAIL`, `USERS`, `USER_ADD`, `SETTINGS`, `UPLOAD`, `DELETE`) instead of
shipping private bitmap resources. They resolve the active theme color when
painted. The overloads without an icon remain source-compatible and receive the
neutral `DOCUMENT` symbol automatically.

`StandardView` is the recommended optional layout primitive for the component
returned by a view contribution. It standardizes the outer spacing and header line
without restricting the plugin's content. Its header action overload keeps actions
and the centered title in one row.

A complete visual theme implements `ThemeProvider` and returns every semantic
`ThemeColor`, `ThemeFont` and `ThemeSpacing` value. Register it with
`context.themes().register(provider)` and pass the handle to `closeOnStop`. The
bundled `plugins.community.darktheme` is the SDK-only reference. Views consume
`context.themes().current()`; they never import the provider or its concrete colors.

## Localization

`PluginContext.messages()` resolves keys from the plugin entry-point package for
the active runtime language and falls back to English. Every plugin supplies
`messages_en.properties`; bundled plugins also maintain the complete German
baseline. Long-lived views subscribe through `LocalizationService.onChange(...)`
and register that handle with `closeOnStop(...)`. See
[`localization.md`](localization.md) for bundle naming and quality gates.
