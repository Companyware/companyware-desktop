# Companyware Desktop developer guide

This guide is the practical entry point for developing the Java desktop platform
and its plugins. Architectural decisions and work-package history live in the
separate `companyware-knowledge` repository.

Companyware is the product family. This repository implements **Companyware
Desktop**. **Companyware Web** is an independently built and released Symfony
application; it neither consumes Desktop `.cwp` packages nor shares the Desktop
database. Integration requires separately versioned API contracts.

## Development baseline

- Java 8 source and target; use a JDK, not only a JRE, for command-line builds.
- Spring Boot 2.1.9, Swing, Hibernate and Apache Derby.
- Gitolite `develop` is the development authority. GitHub `main` is published only
  deliberately at a suitable release state.
- Build with `mvnw.cmd clean verify` on Windows or `./mvnw clean verify` elsewhere.

## Source checkout, build and start

1. Clone the official source repository and enter its root directory:

   ```powershell
   git clone https://github.com/Companyware/companyware-desktop.git
   Set-Location companyware-desktop
   ```

2. Select a 64-bit JDK 8 through `JAVA_HOME` and verify `java -version` and
   `javac -version`. A JRE alone can run a release but cannot build the source.
3. Build and run the complete test suite:

   ```powershell
   .\mvnw.cmd clean verify
   ```

4. Start the resulting development build:

   ```powershell
   java -jar target\Companyware.jar
   ```

   The default embedded database is created below `data/companyware` relative to
   the working directory. Use a separate test directory or the documented
   `companyware.persistence.embedded.path` property; never point a development
   run at production data.

5. For plugin development, first install the matching SDK classifier locally
   with `.\mvnw.cmd clean install`, then follow
   [`standalone-plugin-starter.md`](standalone-plugin-starter.md). The generated
   project builds a `.cwp` package; it is not a second copy of the Desktop app.

The repository is the developer deliverable. End users should instead download
the versioned Windows ZIP from GitHub Releases and use `start-companyware.cmd` as
described in the user guide.

## Eclipse development versus user deployment

Eclipse runs compiled classes and resources from `target/classes`; this is a
development layout, not a distributable installation. Never assemble a release
by copying individual Eclipse output folders. The supported deployment build is
reproducible and packages mappings, plugin descriptors, classes and dependencies
into the executable Spring Boot JAR. It then creates `Companyware.exe` from that
tested JAR with Launch4j before assembling the Windows ZIP.

From PowerShell run:

```powershell
.\tools\verify-framework-release.ps1
```

For a double-clickable Windows entry point use:

```text
build-windows-release.cmd
```

In Eclipse, add it through **Run → External Tools → External Tools
Configurations → Program**:

- Location: `${workspace_loc:/Companyware/build-windows-release.cmd}`
- Working Directory: `${workspace_loc:/Companyware}`
- Arguments: `--no-pause`

Select the JDK 8 in Eclipse and ensure the launched environment exposes the same
`JAVA_HOME`. A successful run prints `RELEASE BUILD PASSED` and writes both
`Companyware.jar` and `Companyware.exe` below the versioned directory in
`target/release`. A red Maven test or gate leaves no approved
release even if an older ZIP is still present.

Launch4j creates a native Windows launcher, not a bundled Java runtime or an MSI
installer. The resulting EXE starts the adjacent `Companyware.jar`; both files
must remain in the same directory, and Java 8 or newer is required on the target
computer. An installer with a private runtime can be evaluated as a separate
future packaging step.

The supported public-framework boundary, compatibility promise, MIT header policy
and publication gates are defined in [`framework-release.md`](framework-release.md).
Run `tools/verify-framework-release.ps1` before preparing a framework release.

Import the repository root as an **Existing Maven Project** in Eclipse. Select a
Java 8 JDK, run **Maven → Update Project**, then launch the `Companyware` main class.

## Persistence profiles

Private installations use embedded Derby by default:

```text
-Dcompanyware.persistence.profile=embedded
-Dcompanyware.persistence.embedded.path=data/companyware
```

Companyware acquires an application lock next to the configured embedded database
before Spring and Derby start. A second desktop process is rejected with a clear
message; never delete Derby's `db.lck` or the Companyware lock to bypass this check.
The client-server profile deliberately has no local instance lock, because several
desktop clients may connect concurrently.

For compatibility with an older running Companyware version that does not yet own
the application lock, startup also probes Embedded Derby before Spring/Hibernate.
Derby's specific already-booted failure is translated into the same short
second-instance message; unrelated connection failures remain visible for diagnosis.

For several clients, operate Derby Network Server independently and add these VM
arguments to the Eclipse launch configuration:

```text
-Dcompanyware.persistence.profile=client-server
-Dcompanyware.persistence.host=localhost
-Dcompanyware.persistence.port=1527
-Dcompanyware.persistence.database=companyware
-Dcompanyware.persistence.username=app
-Dcompanyware.persistence.password=<deployment password>
```

The desktop process never starts the Network Server. Both profiles run the same
owner/version/checksum migrations. Applied migrations are immutable; schema changes
always receive a higher version. Back up databases as described in
[`derby-backup.md`](derby-backup.md).

If a published SQL resource is moved and only transport formatting changes, the
plugin may declare the previously released checksum through the optional
`PluginMigration` checksum arguments. This compatibility list is only for an
audited, semantically identical migration; every real schema change still requires
a new version.

## Plugin boundaries

### Application ports and future remote adapters

Put each business use case behind an interface in the plugin's `application`
package. Swing views consume that interface and immutable DTOs; they do not execute
SQL or know whether data is local. A local `infrastructure` adapter may use
`PluginDatabase`. The plugin entry point is the composition root that chooses the
adapter and passes the port to the view.

Use `ApplicationResult<T>` for outcomes crossing this boundary. Its stable error
codes are `VALIDATION`, `NOT_FOUND`, `CONFLICT`, `UNAVAILABLE` and `INTERNAL`.
Presentation translates those categories into user-facing text and must not expose
database or transport exception messages. DTOs contain values only and never Swing,
JDBC, Hibernate or HTTP types.

Do not add an HTTP client merely to demonstrate readiness. When a versioned remote
contract is actually published, add a remote infrastructure adapter beside the
local one and select it in composition/configuration. The application port, Swing
view and plugin lifecycle remain unchanged; Desktop and Web do not exchange plugin
packages or implementation classes.

A new plugin compiles against `platform.api.*`. It must not import `core.*`,
`desktop.*`, `pluginmanager.*`, `models.*`, another plugin implementation or a
concrete shell/login controller. The descriptor and SDK contract are documented in
[`plugin-sdk-v1.md`](plugin-sdk-v1.md).

The minimum implementation has one required method:

```java
public final class OrdersPlugin implements platform.api.plugin.Plugin {
    @Override
    public void initialize(PluginContext context) {
        context.addView("orders", "Orders", 100, OrdersPanel::new);
        context.addNavigation("orders.navigation", "Orders", 100, "orders");
        context.addMenuItem("orders.refresh", "Refresh", 100, "orders", this::refresh);
        context.addSettingsPage("orders.settings", "Order settings", 100, SettingsPanel::new);
    }

    private void refresh() {}
}
```

Registrations made by the convenience methods are removed automatically when the
plugin stops. For conflicting customizations, numeric order is resolved first,
then plugin ID and contribution ID. Prefer contributions, commands and events over
replacing implementation classes.

Keep the entry class, descriptor and message bundles at the plugin root. Put Swing
components in `view` and SQL scripts in `migration`; do not create empty
`controller` or `model` packages. Larger business plugins may add domain,
application and infrastructure packages when those responsibilities actually
exist. MVC can be used inside the presentation layer, but it is not mandatory
boilerplate.

### Complete HelloWorld example

`plugins.community.helloworld` is the smallest runnable example of an application
area. Its `initialize` method:

1. applies two immutable migrations to its own `HelloWorld` table;
2. registers a lazy Swing view with `addLocalizedView(...)`; and
3. links an OutlookBar destination to that view with
   `addLocalizedNavigation(...)`.

Mehrteilige Funktionsbereiche können davor mit
`addLocalizedNavigationGroup(...)` eine lokalisierte, nicht anklickbare
Bereichsüberschrift registrieren. Die anschließend geordneten View-Ziele bleiben
eigenständige Navigation Contributions und werden eingerückt dargestellt.

Activating the plugin adds **HelloWorld** on the left. Selecting it creates the
right-hand panel and reads `Hello World!` through `PluginDatabase`. Deactivation
removes both contributions immediately. The shell revalidates and repaints the
OutlookBar during the same registry notification; no navigation click or window
change is required to make either lifecycle change visible. Build its package with:

```powershell
.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/helloworld `
  -Descriptor src/main/java/plugins/community/helloworld/Helloworld.yaml `
  -Output target/plugins/helloworld.cwp
```

External plugins use the version-bound standalone starter described in
[`standalone-plugin-starter.md`](standalone-plugin-starter.md). The Desktop Maven
build publishes an `sdk` classifier containing only `platform.api.*`; generated
projects pin that exact version and create their own two-entry `.cwp`. Do not copy
SDK sources or depend on the complete Desktop application JAR.

Plugins may create and query tables they own through `PluginDatabase`. They may
also add views, navigation, menu commands, dialogs and settings pages through the
public UI registry. They must not import Hibernate, Derby drivers or shell classes.

Direct replacement of an existing class or interception of arbitrary SQL is not a
supported extension point. Such replacement is ambiguous when several plugins try
it and bypasses lifecycle cleanup. A deliberate customization therefore needs a
published service, command, event or ordered contribution hook. Current ordering
is deterministic; it orders contributions but does not silently replace duplicate
IDs. Add a new typed hook when a use case must change another module's behavior.

## Security contracts

Plugins find typed services with `SecurityServiceIds`:

- `AUTHENTICATION` establishes or ends the current session.
- `AUTHORIZATION` exposes the current immutable identity and permission checks.
- `AUDIT` records security-relevant actions without credentials.
- `SECRETS` retrieves deployment secrets kept outside source and descriptors.

Create an owned permission with `context.permission("orders.edit")`. Its external
ID becomes `<plugin-id>:orders.edit`. Attach it to an advanced UI contribution with
`AccessRequirement.permission(permission)`. A wildcard permission is reserved for
the built-in administrator.

Initial passwords must be changed before authentication completes. Passwords use
BCrypt; clear-text passwords and secret values must never be logged or persisted.
All password entry points use `core.security.PasswordPolicy`: at least ten
characters are required and the shipped bootstrap password `companyware` can never
be assigned again. Administrative resets set `passwordChangeRequired`; a user
changes their own password through the typed `AuthenticationService` in the profile
view. Keep this rule centralized when adding another credential UI.
The default secret provider reads `CW_SECRET_<NAME>` or the local JVM property
`companyware.secret.<name>`.

Logout is centralized in `LoginController.logoutAndShowLogin()`. Every UI entry
must use this path. It clears authenticated views and status, disables navigation
while the modal login is open, calls `AuthenticationService.logout()` and
`MutableSecretProvider.clearSessionValues()` (which zeroes retained arrays), then
creates a new empty `LoginView`. It deliberately leaves plugins, Spring and Derby
running.

`RecoveryKeyService` owns the local recovery-key protocol. The displayed value is
`selector.secret` with independently generated high entropy. Derby stores only the
selector, BCrypt hash, status, attempts, lock and timestamps. Verification locks
the row and commits password replacement plus key consumption atomically. Never
log the submitted key, disclose account existence before success, or replace this
flow with unverified e-mail. Schema migration 6 is shared by Embedded and Network
Server because both profiles run `SecuritySchemaMigration` through the same
connection provider.

The same service owns verified-address recovery. Identity migrations 7–9 add the
verification flag/time and `AccountEmailTokens`. Verification and reset tokens
contain an independent selector and secret, expire after 30 minutes, are BCrypt
hashed at rest, lock after repeated failures and are consumed in the same database
transaction as verification or password replacement. Requests by e-mail always
return the same UI result. Only one active, uniquely matching, verified account is
eligible. Changing a profile address immediately clears its verified status.

`RecoveryMailSender` is the test seam; `SmtpRecoveryMailSender` uses JavaMail and
requires TLS by default. The desktop accepts the complete configured link in its
dialog, so no operating-system protocol registration is assumed. The offline
recovery key and administrative reset remain independent fallback paths.

## Themes and Swing

Plugins consume `ThemeProvider`, `ThemeRegistry` and semantic tokens from the SDK.
Do not import `core.ui.CompanywareTheme` or hard-code brand colors where a semantic
token exists. The light Core theme is the protected fallback; partial overlays are
applied deterministically.

`plugins.community.darktheme` is the complete second-theme reference. It implements
`ThemeProvider` using only `platform.api.theme`, Java AWT and the plugin lifecycle,
then registers the returned handle with `PluginContext.closeOnStop(...)`. Base themes
must supply every color, font and spacing token. Theme selection is activated through
`ThemeRegistry.activate(...)`, applied on the Swing EDT and persisted by the Desktop
registry. If a selected provider stops or is unavailable at startup, the protected
light provider remains active. Plugin views read semantic tokens—for example
`ThemeColor.SUCCESS`—instead of copying a palette value.

Lifecycle activation is reentrancy-safe. A provider registration may synchronously
refresh Swing and cause an editor callback before the original start call returns;
the plugin manager and lifecycle coalesce that nested request instead of creating a
second runtime. Registry duplicate-ID checks remain strict and must not be weakened
to mask repeated plugin starts.

Theme tokens and registry listeners change on the EDT immediately. Refreshing the
open Swing window trees is queued for the next EDT turn so activation from a table
cell can finish committing its editor value before FlatLaf reinstalls the table UI.
Menu defaults cover normal, selected, disabled and accelerator text for menus,
popup menus and checkbox/radio menu items; keep all of them mapped to semantic
text, muted-text, surface and accent tokens when extending the theme bridge.
Because Swing popup menus may be created before they are attached to a visible
window, the Desktop theme bridge also walks each `JMenu` popup subtree explicitly
after updating the window UI. Do not rely on `updateComponentTreeUI(window)` alone
for consistent opened and not-yet-opened menus.

Values installed into `UIManager` must be `ColorUIResource`/`FontUIResource`
instances. Plain AWT values become explicit component properties when a Swing UI
delegate installs them and are then retained across the next look-and-feel change,
causing a mixed Light/Dark component tree. The public resolved theme tokens remain
ordinary AWT values; only the Desktop Swing bridge wraps them as UI resources.
Conversely, an intentionally styled control such as the primary Login button must
copy its semantic color to a plain AWT value so a later UI delegate refresh does not
replace that one component's primary background while retaining its explicitly
contrasting text.
Views must not install a look and feel themselves; the Desktop theme registry owns
that application-wide decision.

The Desktop mapping applies the selected semantic palette to FlatLaf component
defaults, including menus, fields, tables, borders, selection and focus indication.
When adding custom painting, resolve the current token when painting or subscribe to
`ThemeRegistry.onChange(...)`; do not cache a concrete color across theme changes.

All Swing creation and mutation must occur on the Event Dispatch Thread. Registry
implementations already serialize registration and notifications onto that thread.
After changing the component tree, shell containers must call both `revalidate()`
and `repaint()` so runtime plugin changes become visible immediately.
`StandardView` uses a shared top inset so its content begins level with the first
OutlookBar destination. Core overview screens use the same primitive as SDK views.
Legacy sidebar destinations call `JOutlookBar.setSelectedEntry(...)`; the shell
owns the selected-state rendering. The bottom profile action is installed with
`setFooterAction(...)`, keeping it independent from plugin navigation order.
The OutlookBar renders every ordinary destination in its top navigation group;
only the persistent profile action uses the separate footer area. Consecutive
destinations following an SDK navigation group are rendered inside that clickable
OutlookBar item and become visible only while the group is selected. The expanded
child panel is inserted immediately after the group button, before subsequent
top-level destinations. Every top-level button, including a group button, uses
the same 50-pixel navigation style; group children use a centered 72-pixel
icon-and-label tile. Use `platform.api.ui.CompanywareIcons` for the standard
semantic symbols so plugins share one size, stroke and theme behavior. An omitted
child icon falls back to `CompanywareIcons.DOCUMENT`; top-level items remain
text-only. Custom icons are allowed only when the domain needs a symbol not covered
by the standard set and should use the same 30-pixel canvas.

Core uses the same public set for the **Einstellungen** children, plugin-package
upload and table actions. `EDIT` uses the accent color; destructive `DELETE` uses
the semantic warning color. Do not reintroduce private pencil, cross, user or
upload bitmap assets for these actions.

Swing exit handlers delegate to `ApplicationShutdown` instead of invoking
`System.exit` on the Event Dispatch Thread. The application explicitly closes the
Spring context while AWT is still running and invokes `System.exit` only after
plugin-owned UI registrations, Hibernate/Derby and the embedded-instance guard
have been closed. Spring's automatic JVM shutdown hook is disabled to avoid a
second, competing cleanup path.

## Resource ownership and cleanup

Every acquired resource has one explicit owner. Hibernate `Session` instances,
JDBC statements and result sets, resource streams, archive file systems and
directory streams use `try-with-resources`. Provider-owned JDBC connections are
returned through Hibernate's `ConnectionProvider`; plugin code must never call
`Connection.close()` on these borrowed connections. The platform resets transaction
state and returns the connection even if work, rollback or reset fails, preserving
the original error and attaching cleanup errors as suppressed exceptions.

Plugins register listeners and other long-lived handles with
`PluginContext.closeOnStop(...)`. The context closes them in reverse registration
order. Built-in plugins use the process-owned shared `PluginClassLoader`, which
closes after all runtimes stop. Every trusted-local installed package instead gets
one dedicated `PackagePluginClassLoader` when its runtime starts. The runtime owns
that loader and closes it after plugin `stop()` and context cleanup, including a
failed start; deactivation therefore releases the package JAR before replacement.
Shutdown remains idempotent. Do not create an unmanaged `URLClassLoader`, thread,
database session or stream in a plugin.

`PluginManager.installTrustedLocal(Path)` is the live installation and update
boundary. It inspects the complete `.cwp` before extraction. A new ID is persisted
as inactive and added lazily to the lifecycle graph. An existing external ID is
accepted only while inactive, with the same display name and a strictly newer
semantic version. File replacement, lifecycle replacement and metadata persistence
form one rollback boundary: a rejected dependency graph or persistence failure
restores the previous package and definition. Built-in IDs cannot be replaced.
No entry-point class is loaded until the user activates the plugin. On startup,
only directories with
`plugin.yaml`, `plugin.jar` and matching `trusted-local` metadata under
`companyware.plugins.directory` (default `plugins-installed`) are rediscovered.

`PluginManager.uninstallPlugin(String)` accepts only inactive external leaf
plugins. Any installed dependant blocks removal. The package directory is renamed
before lifecycle and metadata removal; a rejected commit restores it. Plugin-owned
database migrations and tables are deliberately retained for rollback and later
reinstallation. Built-in plugins cannot be uninstalled through this contract.

Use `StandardView.create(title, content)` for a normal right-hand workspace. Pass a
third component for an optional header action such as **Plugin installieren**. The
helper gives every plugin the same 16-pixel outer edge, 50-pixel header, centered
heading and 12-pixel separation before its content. This is the default Companyware
layout and keeps tables and forms on one vertical grid. A specialized application
may deliberately build a different full-canvas view, but should document why.

## Localization

Use `PluginContext.messages()` for every visible plugin text. The returned
`PluginMessages` is scoped to the plugin entry-point package and follows the current
runtime locale. Do not cache translated strings; resolve them while creating or
refreshing a view.

Plugins ship `messages_en.properties` as the mandatory fallback and a matching
`messages_de.properties` for the release-gated baseline. French and Spanish use
`messages_fr.properties` and `messages_es.properties`. Subscribe through the typed
`LocalizationService.onChange(...)` when a long-lived view must update in place,
and register the returned handle with `PluginContext.closeOnStop(...)`.

`TranslationBundleCompletenessTest` verifies identical German and English key
sets for every bundled plugin. New visible text is incomplete until its key exists
in both release-gated bundles. Never use translated labels as action commands,
database identifiers or API values.

The current production locales are German, English, French and Spanish. The
planned 28-language catalog is documented in [`localization.md`](localization.md);
additional languages become selectable only after their bundle has been reviewed.
Properties files are UTF-8. Legacy menu contributions must register their message
key through `Menu.addLocalizedItem`; public SDK plugins should subscribe through
`LocalizationService.onChange` and rebuild or update visible components on the EDT.

## Rechnungsplugin

`plugins.community.invoice` folgt den verbindlichen Plugin-Schichten:

- `domain` enthält ausschließlich Positionsvalidierung und deterministische
  `BigDecimal`-Berechnung mit `HALF_UP`-Cent-Rundung;
- `application` veröffentlicht `InvoiceService` sowie unveränderliche Commands
  und DTOs ohne Swing-, JDBC- oder Derby-Abhängigkeit;
- `infrastructure.JdbcInvoiceService` implementiert Entwurf, Lesen und
  Festschreiben über `PluginDatabase`;
- `view` enthält getrennte Swing-Ziele für Übersicht, Neuerstellung, Vorlage und
  Suche sowie den Entwurfseditor; alle verwenden ausschließlich die
  Anwendungsinterfaces;
- die Composition Root registriert den Dienst unter
  `InvoiceServiceIds.INVOICES`, führt die plugin-eigene Migration aus und
  registriert Menü, eine Navigationsgruppe und ihre vier Views über öffentliche
  Plattformverträge.

`InvoiceService` stellt `createDraft`, `updateDraft`, `list`, `find`, `details`
und `finalizeDraft` bereit. Die Übersicht arbeitet nur mit `InvoiceDto`; der
Editor lädt den vollständigen Snapshot als `InvoiceDetailsDto` und sendet
Eingaben als `CreateInvoiceCommand` mit `InvoiceLineInput`. Finalisierte
Snapshots werden auf der Anwendungsgrenze gegen Änderungen geschützt.

`InvoiceDocumentService` bildet die zweite transport-neutrale Grenze für Vorlage,
Vorschau und PDF. `previewTemplateHtml(...)` rendert ungespeicherten Quelltext
gegen einen unveränderlichen, klar als Beispiel gekennzeichneten Snapshot; es
schreibt weder Vorlage noch Rechnungsdaten. `InvoiceTemplateEngine` akzeptiert höchstens 256 KiB
wohlgeformtes XHTML aus einer festen Element-/Attributliste, eingebettetes CSS und
eine feste Platzhalterliste. Es blockiert DTDs, Entitäten, Skripte, externe
Ressourcen, CSS-Imports/-URLs und unbekannte Platzhalter. Alle Snapshotwerte werden
HTML-escaped; nur der intern erzeugte Positionsblock wird als Markup eingesetzt.

`previewTemplatePdf(...)` rendert denselben ungespeicherten Beispieldatenstand als
deterministische PDF-Nutzlast. `InvoiceTemplateEditorPanel` rendert diese über die
gemeinsame `PdfPagePreview` direkt auf der rechten Editorseite. Die zusammengefasste
Aktion **PDF-Vorschau aktualisieren** ersetzt dort die frühere HTML-Aktualisierung
und den separaten externen PDF-Aufruf; die Vorlage wird dabei nicht gespeichert.
Eine fehlerhafte Aktualisierung ersetzt die letzte gültige Vorschau nicht.

`previewPdf(invoiceId)` erzeugt für Entwürfe und festgeschriebene Rechnungen die
PDF-Nutzlast aus derselben kontrollierten Vorlage. `InvoicePreviewPanel` rendert
diese Bytes ebenfalls mit `PdfPagePreview` und PDFBox seitenweise in der Anwendung;
es verwendet keine separate Swing-HTML-Darstellung. Für eine festgeschriebene
Rechnung liefert `createPdf(...)`
dieselben Bytes mit dem endgültigen Dateinamen, bleibt aber der geschützte
Exportpfad. Dadurch entsprechen Logo, Seitenumbruch und Footer in Vorschau und
Export exakt einander.

Die weiterhin im Briefpapiereditor verwendete Swing-HTML-Komponente unterstützt
keine `data:`-Bildadressen. `SwingHtmlPreviewSupport` materialisiert dort das vom
Dokumentdienst erzeugte Logo vorübergehend als lokale PNG-/JPEG-Datei und ersetzt
dessen Adresse nur in der Anzeige-Kopie. Beim Aktualisieren oder Schließen der
Ansicht wird die Datei entfernt; gespeicherte Vorlage und PDF-HTML bleiben
unverändert.

Das Briefpapier-Singleton enthält zusätzlich wiederverwendbare Verkäufer-Vorgaben
und Footertext. `previewBrandingHtml(...)` validiert und rendert den noch nicht
gespeicherten Formularstand gegen die aktuelle Vorlage. Neue Entwürfe lesen diese
Vorgaben; bestehende Rechnungen behalten ihre Verkäufer-Snapshots. Der Footer ist
Dokumentgestaltung und wird über `{{company.footer}}` eingesetzt.

Die aktuelle Vorlage liegt als Singleton in `CW_INVOICE_TEMPLATE`. Fehlt dort ein
Eintrag, wird `default-template.xhtml` verwendet. OpenHTMLToPDF/PDFBox rendert aus
demselben expandierten XHTML wie die Swing-Vorschau. Metadaten und Dokument-ID
werden anschließend aus stabilen Rechnungsdaten normalisiert, sodass dieselbe
Vorlage und derselbe Snapshot bytegleiche PDF-Ausgaben liefern. Geldbeträge und
Nummern stammen weiterhin ausschließlich aus `InvoiceService`.

Der ausgelieferte Standard orientiert sich am freigegebenen Referenzbriefpapier.
`default-logo.png` liegt als Klassenpfad-Ressource im Rechnungsmodul und wird nur
verwendet, wenn noch kein `CW_INVOICE_BRANDING`-Datensatz existiert. Damit bleiben
vorhandene eigene Logos und auch eine bewusst gespeicherte Logo-Entfernung
unverändert. Das Standardlogo wird als validiertes PNG mit 65 mm Breite und
Rechtsausrichtung durch denselben internen Data-URI-Pfad gerendert wie ein später
in der Oberfläche gespeichertes Logo.

Das Briefpapierlogo liegt als validiertes PNG/JPEG-BLOB in der Singleton-Tabelle
`CW_INVOICE_BRANDING`; gespeichert werden außerdem Breite in Millimetern und
Ausrichtung. `InvoiceDocumentService` bietet Lesen, Speichern und Entfernen über
`InvoiceBrandingDto`. Der Renderer erzeugt allein intern das Data-URI-Markup für
`{{company.logo}}`; Vorlagen dürfen weiterhin keine eigenen `img`-Elemente,
Dateipfade oder Netzwerkadressen enthalten.

Ein Entwurf besitzt keine Rechnungsnummer. `finalizeDraft(...)` sperrt und prüft
den Entwurf, erhöht den jahresbezogenen Nummernkreis und schreibt Status, Nummer
und Ausstellungsdatum innerhalb desselben `PluginDatabase.write`-Callbacks. Ein
Rollback setzt damit auch die Nummernkreiserhöhung zurück. Nummern haben das Format
`<SERIE>-<JAHR>-<SECHSSTELLIGE FOLGE>`; Serien werden normalisiert und erlauben nur
ASCII-Buchstaben, Ziffern, `_` und `-`.

Die Tabellen `CW_INVOICE`, `CW_INVOICE_LINE` und `CW_INVOICE_SEQUENCE` gehören
dem stabilen Plugin `companyware.community.invoice`. Anwendungs- oder UI-Code darf
ihre SQL-Struktur nicht direkt verwenden. Weitere Use Cases werden über
`InvoiceService` ergänzt, damit ein späterer Remote-Adapter dieselbe Grenze nutzen
kann.

`EInvoiceService` ist die transportneutrale Grenze für strukturierte Belege.
`JdbcEInvoiceService` erzeugt XRechnung-UBL und ZUGFeRD-CII, prüft XML defensiv,
bettet CII als `factur-x.xml` mit AF-Beziehung, PDF/A-3-Kennung und sRGB-OutputIntent
in das visuelle PDF ein und speichert PDF, XML und SHA-256 unveränderlich in
`CW_EINVOICE_ARCHIVE`. `CW_INVOICE_CORRECTION` verbindet Original und
Korrekturbeleg; das Original wird erst nach erfolgreicher Archivierung storniert.
Die lokale Prüfung schützt Syntax, Dokumenttyp und Belegidentität. Eine
vollständige Konformitätsprüfung gegen die jeweils amtlichen XSD-/Schematron-Regeln
bleibt ein externer Release- bzw. Versand-Gate.

`InvoiceMailService` trennt gespeicherte Vorlage, gerenderten Prüfdraft und den
expliziten Versand. `JdbcInvoiceMailService` archiviert den Beleg vor dem Senden,
protokolliert erst nach erfolgreichem Transport und kennt keine SMTP-Zugangsdaten.
`MailTransport` ist der testbare Infrastruktur-Seam; die Produktionsimplementierung
nutzt JavaMail mit STARTTLS. Betreff und Nachricht sind kontrollierter Klartext,
nicht HTML. Das Protokoll speichert nur Rechnung, Empfänger, Betreff, Zeitpunkt
und die XML-Anhangentscheidung.

Die Oberfläche **Einstellungen → E-Mail-Versand** persistiert ausschließlich die
nicht geheimen Werte in `CW_INVOICE_SMTP_SETTINGS`. `SmtpSettingsDto` bleibt Teil
der transportneutralen Mail-Anwendungsgrenze. Das Passwort gelangt als `char[]`
zur Anwendung und wird nach jedem Speichern oder Test überschrieben.
`MutableSecretProvider.setForSession(...)` legt es nur im Prozessspeicher ab;
`SecretProvider` kann weiterhin einen durch die Betriebsumgebung gelieferten Wert
bereitstellen. Der Testpfad öffnet und schließt eine echte authentifizierte
JavaMail-Verbindung, sendet aber keine Nachricht.

Migration 8 ergänzt `SMTP_FROM_NAME` getrennt von `SMTP_FROM`. Der SMTP-Adapter
erzeugt daraus mit UTF-8-Kodierung eine `InternetAddress`, sodass Anzeigename und
Absenderadresse nicht mehr vermischt werden. Leere Anzeigenamen bleiben für
bestehende Installationen kompatibel und führen weiterhin zu einer reinen
Absenderadresse. `restoreDefaultLogo()` setzt nur die gebündelten Logo-Bilddaten,
Breite und Ausrichtung zurück; gespeicherte Firmendaten und Footer bleiben erhalten.

## Testing a change

At minimum:

1. Run `mvnw.cmd clean verify` with a JDK 8.
2. Import/refresh in Eclipse and confirm there are no compilation errors.
3. Launch with a disposable embedded database and test first-run/login behavior.
4. For persistence changes, test both embedded and external Network Server modes.
5. For plugin changes, test startup, live installation, activation/deactivation,
   dependency guards, per-package JAR release and failure cleanup.

`ResourceLifecycleArchitectureTest`, `JdbcResourcesTest` and
`PluginClassLoaderTest` protect the resource-ownership rules. Add a focused failure
test whenever a new resource type or cleanup phase is introduced.

Never commit `data/`, `target/`, Eclipse workspace metadata, passwords, private keys,
generated archives or dependency caches.
