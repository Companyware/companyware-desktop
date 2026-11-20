# AI Plugin Builder: Architektur- und Integrationsanalyse (AP 01)

- Status: Analyse abgeschlossen, keine Implementierung
- Beobachtet am: 2026-10-21
- Source-Baseline: `dbc5c7f4d73cfef8c79ef9e5a08d8d7b928d3458`
- Knowledge-Baseline: `3f39bdfa39acccc114c83275d5dbdfc8d0657772`
- Zielplattform: Companyware Desktop, Java 8, Swing, Maven, lokaler Codex-Prozess

## 1. Ergebnis in Kürze

Der Plugin Builder kann auf der vorhandenen Plugin-Runtime aufbauen. Er benötigt
keine zweite Plugin-Architektur. Bereits vorhanden sind das öffentliche SDK unter
`platform.api.*`, ein vollständiges HelloWorld-Beispiel, sichere nicht ausführende
Paketprüfung, atomare Trusted-Local-Installation, Live-Erkennung, Aktivierung,
Deaktivierung, Update und Deinstallation sowie die Swing-Pluginverwaltung.

Neu benötigt werden ein eigenständiger Starter für externe Maven-Projekte, die
Erzeugung des Codex-Kontexts, eine Builder-Ansicht, ein kontrollierter
Prozessadapter, eine Status-/Abbruchsteuerung und eine explizite Build- und
Prüfkette. Der heutige PowerShell-Paketbauer ist wiederverwendbar als Referenz,
setzt aber kompilierte Klassen im Hauptprojekt voraus und ist daher noch kein
eigenständiger Plugin-Starter.

Für AP 05/06 wird eine zweistufige Entscheidung empfohlen:

1. `codex exec` ist die kleinste belastbare Integration für einen ersten,
   abgeschlossenen Entwicklungsauftrag. JSONL-Ereignisse können gelesen und der
   Prozess kann beendet werden.
2. `codex app-server` ist der Zielkandidat, sobald Companyware fortlaufende
   Ereignisse, Unterbrechen/Fortsetzen und interaktive Genehmigungsanfragen
   produktgerecht abbilden soll. Die lokal beobachtete Schnittstelle ist noch als
   experimentell gekennzeichnet; ihr Protokoll muss vor AP 05 festgeschrieben und
   durch einen Kompatibilitätsadapter gekapselt werden.

AP 02 kann unmittelbar begonnen werden. Zuvor sind keine Änderungen an der
Plugin-Runtime notwendig.

## 2. Tatsächlich vorhandene Bausteine

### 2.1 Öffentliches Plugin-SDK

Vorhanden:

- `platform.api.plugin.Plugin` und `PluginContext` bilden den Einstieg und den
  lebenszyklusgebundenen Kontext.
- `PluginDescriptor`, `PluginId`, `PluginDependency`, `PluginState`,
  `PluginHealth` und `PluginDiagnostic` beschreiben Identität, Abhängigkeiten und
  Laufzeitstatus.
- `UiRegistry`, `UiContributionSpec`, `StandardView` und die Convenience-Methoden
  des `PluginContext` erlauben Swing-Views, Navigation, Menüs und Einstellungen
  ohne Import der Desktop-Implementierung.
- `PluginDatabase` und `PluginMigration` kapseln Derby-Profil und Transaktionen.
- Typisierte Services, Events, Lokalisierung, Themes, Autorisierung, Audit und
  Secret-Zugriff liegen ebenfalls unter `platform.api.*`.
- `src/main/resources/plugin-descriptor.schema.json` definiert das Descriptor-v1-
  Format. IDs, SemVer, Entry Point, Capabilities und Abhängigkeiten sind validiert.
- Architekturtests schützen die Abhängigkeit vom öffentlichen SDK.

Wiederverwendung im Builder:

- Der Starter kompiliert ausschließlich gegen ein explizites SDK-Artefakt und
  Java/Swing, nie gegen `core`, `models`, `pluginmanager` oder andere Plugins.
- `PluginContext` bleibt der einzige Einstieg in UI, Datenbank, Services,
  Lokalisierung und Lifecycle-Ressourcen.
- Der Descriptor wird aus der vorhandenen v1-Spezifikation erzeugt und vor dem
  Build lokal validiert.

Neu:

- Das SDK ist derzeit Teil des monolithischen Companyware-Artefakts. AP 02 muss
  festlegen, wie ein externes Projekt es reproduzierbar bezieht. Die kleinste
  Variante ist zunächst ein aus derselben Source-Baseline erzeugtes, nur die
  `platform.api.*`-Klassen enthaltendes SDK-JAR mit dokumentierter Version. Eine
  Kopie der SDK-Quellen in jedes Pluginprojekt wäre nicht wartbar.

### 2.2 HelloWorld als Referenz

Vorhanden:

- `plugins.community.helloworld.Helloworld` implementiert das aktuelle
  `platform.api.plugin.Plugin`.
- `Helloworld.yaml` enthält eine gültige v1-Identität, Version, API-Version,
  Capability und Frame-Abhängigkeit.
- Zwei unveränderliche SQL-Dateien demonstrieren versionierte Migrationen.
- `GreetingService` und `JdbcGreetingService` trennen Anwendung und lokale
  Persistenz.
- `HelloWorldPanel` verwendet `StandardView` und semantische Theme-Werte.
- Sprachdateien liegen beim Einstiegspaket.

Wiederverwendung im Builder:

- HelloWorld ist die fachlich kleinste Vorlage für Paketstruktur, Descriptor,
  Migration, View, Navigation, Lokalisierung und Tests.
- AP 02 soll die Vorlage nicht direkt im Haupt-Source-Baum kopieren, sondern als
  versioniertes Starter-Template materialisieren. Platzhalter müssen mindestens
  Plugin-ID, Anzeigename, Java-Paket, Entry-Point, Version, Autor und Capability
  abdecken.

### 2.3 Paketbau und Installation

Vorhanden:

- `tools/build-plugin-package.ps1` erstellt ein `.cwp` mit `plugin.yaml` und
  `plugin.jar`.
- `PluginPackageInspector` prüft die Erweiterung, Paketstruktur, Descriptor,
  API-Kompatibilität, Pfade, Duplikate und Ressourcenlimits ohne Klassen zu laden.
- `PluginPackagePolicy.DEFAULT` begrenzt auf 1.024 Einträge, 50 MiB je Eintrag,
  100 MiB entpackte Gesamtgröße und Kompressionsverhältnis 100.
- `PluginPackageInstaller` führt Trusted-Local-Installation, Staging, atomaren
  Austausch und Rollback aus.
- `PluginManager.installTrustedLocal(Path)` ist die vorhandene Installationsgrenze.
- `PluginManagerImpl` ergänzt das geprüfte Paket als inaktive Definition zum
  laufenden Dependency-Graphen. Externe Plugins erhalten bei Aktivierung einen
  lifecycle-eigenen Classloader.
- Updates verlangen gleiche Identität, höhere Version und inaktiven Zustand;
  Deinstallation schützt eingebaute und abhängige Plugins.

Einschränkung:

- Das vorhandene Buildskript liest Klassen aus `target/classes` des
  Companyware-Hauptprojekts. Ein externes Projekt braucht einen eigenen Maven-
  Lifecycle, der nur seine Pluginklassen und Ressourcen in `plugin.jar` packt und
  anschließend dieselbe `.cwp`-Wurzelstruktur erzeugt.
- Ein erfolgreicher Build und selbst eine erfolgreiche Paketprüfung sind keine
  Aussage über die Vertrauenswürdigkeit des Java-Codes.

### 2.4 Pluginverwaltung und passender UI-Einstieg

Vorhanden:

- `plugins.core.plugins.view.Plugins#createTableOverview()` baut die aktuelle
  Pluginmanager-Ansicht mit `StandardView`.
- Der Header enthält bereits **Plugin installieren**.
- `PluginsController` verarbeitet die Aktionen `Pluginmanager` und
  `installPlugin`, fordert vor der Trusted-Local-Installation Vertrauen an und
  zeigt Prüfergebnis beziehungsweise Fehler.
- `pluginmanager.plugininterfaces.PluginManager` ist die bestehende Fassade für
  Zustand, Diagnosen, Aktivierung, Installation und Deinstallation.

Empfohlener Einstieg:

- Neben **Plugin installieren** wird in AP 04 die Aktion
  **Neues Plugin mit Codex erstellen** in den Header der vorhandenen
  Pluginmanager-Ansicht aufgenommen.
- Sie öffnet eine eigene `StandardView` oder einen klar abgegrenzten Wizard. Die
  bestehende Tabelle und ihre Aktivierungseditoren werden nicht mit lang laufenden
  Prozesszuständen vermischt.
- Die neue View bleibt Präsentation. Projektanlage, Codex-Prozess, Build und
  Prüfung liegen hinter kleinen anwendungsnahen Interfaces.

## 3. Externes Pluginprojekt

Das Entwicklungsprojekt liegt außerhalb des Companyware-Hauptrepositories. Der
Nutzer wählt ein Elternverzeichnis; Companyware erzeugt darunter ein neues,
kanonisch validiertes Verzeichnis. Existierende Dateien werden nie überschrieben.

Empfohlene Struktur für AP 02:

```text
inventarverwaltung/
  AGENTS.md
  README.md
  pom.xml
  plugin.yaml
  src/main/java/de/example/companyware/inventory/InventoryPlugin.java
  src/main/java/de/example/companyware/inventory/view/InventoryPanel.java
  src/main/resources/de/example/companyware/inventory/messages_en.properties
  src/test/java/de/example/companyware/inventory/InventoryPluginTest.java
  target/                         # erzeugt, nicht versioniert
```

Der externe Build benötigt:

- JDK 8, nicht nur eine JRE;
- einen checksum-gepinnten Maven Wrapper;
- das exakt zur `requiredApiVersion` passende SDK-JAR;
- Compiler-Quell- und Zielniveau 1.8;
- Tests, die ohne Laden in der Companyware-Haupt-JVM laufen;
- einen reproduzierbaren Package-Schritt für `plugin.jar` und `plugin.yaml`;
- Ausgabe nach `target/<plugin-id>-<version>.cwp`.

Der aktuelle Notebook-Shellzustand enthält `java`, aber kein `javac`; der
Maven-Wrapper kann deshalb in dieser Umgebung nicht als JDK-8-Buildnachweis
dienen. Die vorhandenen Projektaufzeichnungen belegen vollständige Builds auf
einem dafür eingerichteten JDK-8-System. Die Builder-UI muss JRE und JDK
unterscheiden und einen fehlenden Compiler vor dem Start verständlich melden.

## 4. Codex-CLI: beobachtete Möglichkeiten

Lokal beobachtet wurde `codex-cli 0.163.0-alpha.10.2`. `codex login status`
meldet eine vorhandene ChatGPT-Anmeldung. Companyware soll nur diesen Status
abfragen und den Benutzer bei Bedarf zur normalen Codex-Anmeldung auffordern. Es
liest, speichert oder überträgt keine Tokens, API-Schlüssel oder ChatGPT-
Zugangsdaten.

### 4.1 Minimaler nichtinteraktiver Aufruf

Die installierte CLI unterstützt:

- `codex exec` für nichtinteraktive Aufträge;
- `-C/--cd <DIR>` für das Arbeitsverzeichnis;
- Prompt als Argument oder über Standard Input;
- `--sandbox read-only|workspace-write|danger-full-access`;
- `--ask-for-approval on-request|never`;
- `--json` für JSONL-Ereignisse auf Standard Output;
- `--output-schema` und `--output-last-message` für maschinenlesbare Ergebnisse;
- einen normalen Prozess-Exitcode sowie getrennte Standard- und Fehlerausgabe;
- `resume` und `fork` für gespeicherte Sessions;
- `--ephemeral`, wenn keine Sessiondateien persistiert werden sollen.

Kleinster AP-05-Prototyp:

```text
codex exec -C <projekt> --sandbox workspace-write --ask-for-approval never --json -
```

Der Prompt wird über Standard Input übergeben. `ProcessBuilder` erhält eine
Argumentliste; es wird keine Shell-Befehlszeile zusammengesetzt. Der Prozess läuft
mit dem Pluginprojekt als einzigem schreibbaren Arbeitsbereich. Die
`danger-full-access`- und Bypass-Optionen werden nicht angeboten.

`--ask-for-approval never` ist nur für einen eng begrenzten, vorab erklärten
Batch-Lauf vertretbar: abgelehnte oder außerhalb der Sandbox liegende Aktionen
scheitern und werden Codex zurückgemeldet. Companyware darf nicht versuchen,
Genehmigungen automatisch zu umgehen. Falls der gewünschte Workflow echte
Genehmigungsdialoge benötigt, ist der Batch-Aufruf nicht ausreichend.

### 4.2 Fortschritt, Genehmigungen und Abbruch

Für AP 06 gibt es zwei Integrationsstufen:

- `codex exec --json`: Companyware liest JSONL zeilenweise, stellt nur bekannte
  Ereignistypen strukturiert dar und bewahrt unbekannte Typen als neutralen
  Protokolleintrag. Abbruch erfolgt zunächst über `Process.destroy()`, nach einer
  kurzen Frist über `destroyForcibly()`. Beide Streams müssen parallel gelesen
  werden, damit der Kindprozess nicht blockiert.
- `codex app-server`: Die offizielle OpenAI-Dokumentation empfiehlt diese
  Schicht für Anwendungen, die lokale Codex-Prozesse, offene Unterhaltungen,
  gestreamte Ereignisse, Unterbrechungen und Genehmigungsantworten verwalten. Die
  lokale CLI kennzeichnet sie als experimentell und bietet unter anderem
  `stdio://` sowie schema-generierende Befehle. Vor produktiver Nutzung sind
  Protokollversion, Ereignisschema, Authentifizierung des lokalen Transports und
  Upgrade-Verhalten verbindlich zu testen.

Die OpenAI-Dokumentation ordnet `codex exec` abgegrenzten nichtinteraktiven Jobs
zu und `app-server` eingebetteten Produkterlebnissen mit Lifecycle- und
Approval-Steuerung:

- <https://developers.openai.com/blog/codex-as-a-platform>
- <https://developers.openai.com/cookbook/examples/codex/build_iterative_repair_loops_with_codex>

Die lokale CLI-Hilfe ist für die tatsächlich installierte Version maßgeblich. AP 05
darf sich nicht darauf verlassen, dass Alpha-/Experimental-Optionen unverändert
bleiben.

### 4.3 Java-8-Tauglichkeit

Die Prozessintegration benötigt keine neue Java-Bibliothek. Java 8 bietet
`ProcessBuilder`, getrennte Stream-Reader, `ExecutorService`, `Future`,
`waitFor(timeout, TimeUnit)`, `destroy()` und `destroyForcibly()`. Swing-Updates
werden mit `SwingUtilities.invokeLater(...)` auf den EDT übertragen.

Java 8 besitzt jedoch keine neueren `ProcessHandle`-Komfortfunktionen. Der Adapter
muss daher Prozessbaumbeendigung unter Windows gezielt testen. Ein bloßes Beenden
des Elternprozesses garantiert nicht in jeder Situation, dass von Codex gestartete
Kindprozesse beendet sind.

## 5. Vorgeschlagene neue Komponenten

Die Namen sind Arbeitsnamen für AP 02–06, keine heute vorhandenen Klassen.

```text
plugins.core.plugins.builder
  application/
    PluginProjectRequest
    PluginProjectService
    PluginDevelopmentRun
    PluginBuildService
  infrastructure/
    StarterProjectGenerator
    CodexCliDetector
    CodexExecProcessAdapter
    MavenPluginBuildAdapter
  view/
    CodexPluginBuilderView
    CodexPluginBuilderController
```

Verantwortungen:

- `StarterProjectGenerator`: validiert Namen/Pfade und erzeugt Dateien atomar in
  einem neuen Verzeichnis.
- `CodexCliDetector`: findet das ausführbare Programm, liest Version und Login-
  Status mit Timeout und redigierter Ausgabe.
- `CodexExecProcessAdapter`: startet genau einen Codex-Prozess, streamt Ereignisse,
  meldet Exitstatus und beendet kontrolliert.
- `MavenPluginBuildAdapter`: startet Wrapper-Build und Tests in einem getrennten
  Prozess. Er lädt oder reflektiert keine generierten Klassen.
- `PluginBuildService`: prüft Ergebnisdateien und übergibt das `.cwp` zunächst nur
  an den vorhandenen `PluginPackageInspector`.
- View/Controller: halten Swing responsiv, zeigen Status, Warnungen und explizite
  Aktionen. Sie enthalten keine Prozess- oder Paketlogik.

Diese Komponenten bleiben zunächst intern im Core-Plugin. Eine neue öffentliche
SDK-Schnittstelle ist für AP 02–08 nicht erforderlich.

## 6. Prozess von der Eingabe bis zur Installation

1. Nutzer öffnet **Einstellungen → Pluginmanager** und wählt
   **Neues Plugin mit Codex erstellen**.
2. Companyware validiert Pluginname, abgeleitete ID, Java-Paket und neues
   Zielverzeichnis.
3. Companyware prüft JDK, Maven Wrapper/SDK und `codex` einschließlich Version und
   Login-Status.
4. Der Starter wird atomar in einem neuen Entwicklungsverzeichnis erzeugt.
5. Companyware erzeugt `AGENTS.md` und eine Aufgabenbeschreibung ausschließlich
   aus SDK-Regeln, lokalen Beispielen und der Nutzereingabe.
6. Der Nutzer startet Codex. Der Adapter begrenzt das Arbeitsverzeichnis auf das
   neue Projekt und streamt Fortschritt sowie Fehler.
7. Codex ändert nur das Entwicklungsprojekt. Companyware führt währenddessen
   weder Maven noch generierte Klassen in der Haupt-JVM aus.
8. Nach Codex-Abschluss zeigt Companyware Zusammenfassung und Dateistatus. Der
   Nutzer gibt den Build separat frei.
9. Ein eigener Prozess führt Wrapper-Tests und Paketbau aus. Timeout, Exitcode und
   Logs werden erfasst.
10. Der vorhandene Inspector prüft das `.cwp` ohne Klassen zu laden. Angezeigt
    werden ID, Name, Version, API-Version, Abhängigkeiten, Hash und Warnungen.
11. Der Nutzer bestätigt die Trusted-Local-Installation ausdrücklich.
12. `PluginManager.installTrustedLocal(Path)` installiert atomar und registriert
    das Plugin inaktiv. Aktivierung ist eine weitere bewusste Aktion.

## 7. Sicherheit und Authentifizierung

### 7.1 Vertrauensgrenzen

- Nutzereingabe und Modelltext sind nicht vertrauenswürdig.
- Das Entwicklungsverzeichnis ist nicht automatisch vertrauenswürdig, obwohl es
  lokal liegt.
- Codex, Maven, Tests, Compiler-Plugins und Testcode sind externe Prozesse mit den
  Rechten des angemeldeten Betriebssystembenutzers.
- `.cwp`-Prüfung schützt Paketstruktur und Kompatibilität, nicht das Verhalten des
  Java-Codes.
- Aktivierter Plugin-Code läuft heute in der Companyware-JVM ohne vollständige
  Isolation. Deshalb bleiben Build, Prüfung, Installation und Aktivierung vier
  getrennte Zustandsübergänge.

### 7.2 Verzeichnis- und Prozessschutz

- Zielpfade werden kanonisch aufgelöst; Root, Home, Source-Repository,
  Installationsverzeichnis und bestehende Nicht-Leer-Verzeichnisse sind verboten.
- Symlinks/Junctions und Pfadwechsel aus dem Projekt müssen vor Schreib- und
  Paketoperationen abgewehrt werden.
- Keine Shell-Komposition und keine ungeprüften Argumente in `cmd /c` oder
  PowerShell-Strings; immer `ProcessBuilder(List<String>)`.
- Umgebung wird minimiert. Companyware reicht keine Datenbankpfade, Secrets,
  privaten Dokumente oder eigenen Zugangsdaten an den Prozess weiter.
- Pro Lauf gelten eindeutige ID, Start-/Endzeit, Timeout, maximale Loggröße und
  expliziter Zustand. Logs redigieren Tokens und bekannte Secret-Namen.
- Netzwerk- und Befehlsfreiheit werden nicht erweitert. Bypass-Flags sind
  ausdrücklich ausgeschlossen.

### 7.3 Codex-Anmeldung

- Companyware ruft nur `codex login status` beziehungsweise einen später stabilen
  Health-Endpunkt auf.
- Anmeldung erfolgt außerhalb der Anwendung über den normalen Codex-Flow.
- Companyware besitzt keine API-Schlüssel-, Token- oder ChatGPT-Credential-Felder.
- Ein fehlender oder nicht angemeldeter Client führt zu einer verständlichen
  Anleitung, nicht zu einem automatischen Login oder einer API-Fallback-Lösung.

## 8. Risiken und offene Entscheidungen

| Thema | Risiko/Entscheidung | Empfehlung vor Umsetzung |
| --- | --- | --- |
| SDK-Verteilung | Das öffentliche SDK ist noch kein separates externes Maven-Artefakt. | In AP 02 ein versionsgebundenes SDK-JAR und dessen Bezugsweg festlegen. |
| Codex-Protokoll | CLI ist eine Alpha-Version; `app-server` ist experimentell. | Adapter kapseln, Mindestversion prüfen, unbekannte JSONL-Ereignisse tolerieren. |
| Genehmigungen | `codex exec` kann keinen vollständigen eingebetteten Approval-Dialog garantieren. | AP 05 als Batch-MVP begrenzen oder vor AP 06 auf getestetes `app-server`-Protokoll wechseln. |
| Prozessbaum | Java 8 beendet Kindprozesse nicht komfortabel plattformweit. | Windows-Integrationstest mit von Codex/Maven gestarteten Kindprozessen. |
| Build-Sandbox | Codex-Sandbox begrenzt nicht automatisch einen später separat gestarteten Maven-Build. | Buildprozess separat begrenzen; keine fremden Maven-Plugins/Repos stillschweigend zulassen. |
| Abhängigkeiten | Ein generiertes Plugin könnte zusätzliche Libraries benötigen. | AP 02 zunächst SDK/JDK-only; Bibliotheks-Bündelung und Lizenzprüfung später explizit entscheiden. |
| Signaturen | Trusted-Local-Pakete sind nicht signiert. | Keine Marketplace-/Drittanbieterfreigabe; Signaturkonzept vor öffentlicher Verteilung. |
| Datenzugriff | Aktivierte Plugins können über erlaubte SDK-Verträge produktive Daten verändern. | Vor Installation statische Prüfung und vor Aktivierung klare Berechtigungs-/Vertrauenswarnung. |
| Pfadwahl | Nutzer kann versehentlich sensible Verzeichnisse freigeben. | Dediziertes Standard-Root mit Allowlist-Logik und verständlicher Pfadanzeige. |
| Lizenz/Provenienz | Generierter oder kopierter Code kann ungeklärte Rechte haben. | README-/Review-Checkliste und keine automatische Veröffentlichung. |

## 9. Technische Voraussetzungen für AP 02–10

### AP 02

- SDK-JAR und Versionierungsweg entscheiden.
- Starter-Template mit Maven Wrapper, Descriptor, Tests und `.cwp`-Packaging.
- Sichere Namens-, ID-, Paket- und Pfadnormalisierung.

### AP 03

- Stabile, kurze SDK-Regeln aus den vorhandenen Dokumenten ableiten.
- `AGENTS.md` und Aufgabenbeschreibung deterministisch erzeugen.
- Keine lokalen Daten, Secrets oder Repository-Historie ungefragt einbetten.

### AP 04

- Neue `StandardView` im Pluginmanager.
- Hintergrundarbeit ausschließlich außerhalb des EDT; alle UI-Updates auf dem EDT.
- Noch keine automatische Ausführung beim Öffnen der View.

### AP 05–06

- Mindestversion und Capability-Erkennung statt Annahmen über CLI-Flags.
- Zuerst `exec`-MVP oder bewusstes `app-server`-Protokoll festlegen.
- JSONL-/Event-Parser, getrennte Streams, Logbegrenzung, Timeout und Abbruch.

### AP 07

- JDK-8-Prüfung mit `javac` und Wrapper-Version vor Buildstart.
- Separater Prozess, reproduzierbarer Build, Tests und Paketprüfung.
- Kein Laden generierter Klassen in Companyware.

### AP 08

- Vorhandenen Inspector und `installTrustedLocal` wiederverwenden.
- Build, Paketprüfung, Installation und Aktivierung getrennt bestätigen.

### AP 09

- Projektmanifest mit Plugin-ID, SDK-Version und letzter gebauter Version.
- Nur vom Nutzer gewählte Entwicklungsprojekte wieder öffnen.
- Versionserhöhung und Diff-Anzeige vor Update erzwingen.

### AP 10

- E2E-Fixture in einem temporären, leeren Root.
- Erfolgs-, Fehler-, Abbruch- und Recovery-Dokumentation auf Deutsch.
- Runtime-Aktivierung nur in einer isolierten Testdatenbank/Testinstallation.

## 10. Notwendige Tests

### Starter und Kontext

- deterministische Ausgabe bei gleichen Eingaben;
- gültige und ungültige Plugin-IDs, Java-Pakete und Windows-Pfade;
- kein Überschreiben bestehender Dateien;
- Schutz gegen `..`, absolute Pfade, Symlinks/Junctions und reservierte Namen;
- generierter Descriptor besteht die vorhandene Validierung;
- generiertes Plugin kompiliert ausschließlich gegen das öffentliche SDK.

### Codex-Prozess

- CLI fehlt, Version unzureichend, Login fehlt und Health-Check hängt;
- Prompt über Standard Input ohne Shell-Escaping;
- Arbeitsverzeichnis ist exakt das Pluginprojekt;
- stdout/stderr mit großen und ungültigen UTF-8-/JSONL-Eingaben;
- unbekannte Ereignistypen brechen die UI nicht;
- Erfolg, Nicht-Null-Exitcode, Timeout, Benutzerabbruch und Prozessbaumende;
- Swing bleibt responsiv und wird nur auf dem EDT verändert;
- Logs enthalten keine simulierten Secrets.

### Build und Paket

- JRE ohne `javac`, falsche Java-Version, Testfehler und Packaging-Fehler;
- keine Ausführung des erzeugten JARs durch die Prüfschritte;
- erwartete `.cwp`-Wurzelstruktur und reproduzierbarer Hash bei gleichen Inputs;
- sämtliche bestehenden `PluginPackageInstallerTest`-Angriffsfälle bleiben grün;
- Installation bleibt inaktiv und erfordert Bestätigung;
- Update mit gleicher/niedriger Version und fremder Identität wird abgelehnt.

### End-to-End

- einfacher Inventar- oder HelloWorld-Auftrag von Projektanlage bis geprüfter,
  inaktiver Installation;
- Abbruch in Codex- und Maven-Phase hinterlässt einen nachvollziehbaren,
  wiederaufnehmbaren Projektzustand;
- bestehende Rechnungs-, Plugin- und Theme-Funktionen bleiben unverändert.

## 11. Kleinste sinnvolle Implementierung

1. AP 02 erzeugt lokal ein eigenständiges, buildbares HelloWorld-Derivat mit
   festem SDK und reproduzierbarem `.cwp`.
2. AP 03 erzeugt nur `AGENTS.md` und einen Prompt als Dateien; noch kein Codex-
   Start.
3. AP 04 bindet die Projektanlage in die Pluginmanager-UI ein.
4. AP 05 startet zunächst einen einmaligen `codex exec`-Lauf mit
   `workspace-write`, JSONL und ohne Bypass-Optionen.
5. AP 06 ergänzt Anzeige und kontrollierten Abbruch. Vor interaktiven
   Genehmigungen wird `app-server` separat prototypisch und versioniert geprüft.
6. AP 07/08 verwenden getrennte Nutzerfreigaben für Build, Prüfung, Installation
   und Aktivierung.

Damit bleibt die erste Lieferung klein, testbar und rückbaubar. Sie verwendet die
vorhandene Runtime, verändert keine Rechnungslogik und setzt keine noch nicht
existierende Desktop-/Symfony-Plugin-Spezifikation voraus.

## 12. Bereits vorhanden versus neu

| Fähigkeit | Bereits vorhanden | Neu zu entwickeln |
| --- | --- | --- |
| Plugin-Runtime/Lifecycle | Ja | Nein |
| SDK-Verträge und Descriptor v1 | Ja | Externer SDK-Bezug |
| HelloWorld-Beispiel | Ja | Parametrisierte Starter-Vorlage |
| `.cwp`-Struktur und sichere Prüfung | Ja | Externes Maven-Packaging |
| Trusted-Local-Installation | Ja | Builder-Vorschau/Bestätigung anbinden |
| Pluginmanager-Ansicht | Ja | Builder-View und Navigation |
| Codex lokal installiert/anmeldbar | Auf diesem System ja | Detektor und Fehlerhilfe |
| Nichtinteraktiver Codex-Lauf | CLI unterstützt `exec` | Java-8-Prozessadapter |
| Streaming/Abbruch | CLI-Prozess/JSONL beziehungsweise `app-server` vorhanden | Robuste UI-Steuerung und Protokolladapter |
| Maven-Build in Fremdprojekt | Noch nicht | Buildadapter und Starter-POM |
| Code-Isolation in Haupt-JVM | Nein | Nicht durch den Builder behaupten; getrennte Freigaben |
| Symfony-Plugin-Unterstützung | Nein | Außerhalb AP 01–10 |

## 13. Freigabe für AP 02

AP 02 kann unmittelbar starten. Der wichtigste erste Akzeptanztest ist nicht die
Codex-Anbindung, sondern ein frisch erzeugtes externes HelloWorld-Projekt, das auf
einem JDK-8-System mit seinem eigenen Wrapper testet, ein gültiges `.cwp` erzeugt
und die vorhandene nicht ausführende Paketprüfung besteht. Erst wenn dieser Pfad
reproduzierbar ist, sollte Codex als Dateierzeuger angeschlossen werden.
