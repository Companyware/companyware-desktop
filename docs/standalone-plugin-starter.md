# Eigenständiges Pluginprojekt erzeugen

## Worum geht es?

Der AP-02-Starter erzeugt ein neues Java-8-Maven-Projekt für ein Companyware-
Desktop-Plugin. Das Projekt kompiliert nur gegen das öffentliche Plugin SDK und
erstellt ein reproduzierbares `.cwp`-Paket. Es installiert oder aktiviert nichts.

## Was wird benötigt?

- ein JDK 8 mit `javac`, nicht nur eine JRE;
- Companyware Desktop `0.29.0-rc.2` aus dem passenden Source-Stand;
- ein neues oder noch nicht vorhandenes Zielverzeichnis;
- PowerShell für den aktuellen Generator.

Installieren Sie zuerst das versionsgebundene SDK-Artefakt in das lokale Maven-
Repository:

```powershell
.\mvnw.cmd clean install
```

Der Desktop-Build erzeugt dabei zusätzlich
`Companyware:Companyware:0.29.0-rc.2:sdk`. Das Classifier-JAR enthält nur
`platform.api.*`. Ein Pluginprojekt darf nicht gegen das vollständige Desktop-JAR
kompilieren.

## Wie geht es?

1. Wählen Sie ein separates Entwicklungsverzeichnis außerhalb der installierten
   Anwendung und außerhalb produktiver Daten.
2. Erzeugen Sie ein Projekt, zum Beispiel:

   ```powershell
   .\tools\new-plugin-project.ps1 `
     -ParentDirectory C:\dev\companyware-plugins `
     -PluginId example.inventory `
     -DisplayName "Inventory" `
     -JavaPackage example.companyware.inventory `
     -EntryClass InventoryPlugin `
     -Author "Example developer"
   ```

3. Wechseln Sie in das ausgegebene Verzeichnis.
4. Führen Sie `.\mvnw.cmd clean verify` aus.
5. Prüfen Sie Source, Testergebnis und das Paket unter
   `target/<plugin-id>-<version>.cwp`.

## Was ist das Ergebnis?

Das neue Verzeichnis enthält einen checksum-gepinnten Maven Wrapper, eine feste
SDK-Version, `plugin.yaml`, eine SDK-only-Beispielklasse, einen JUnit-Test und die
Paketdefinition. Das `.cwp` enthält an seiner Wurzel ausschließlich
`plugin.yaml` und `plugin.jar`. Unveränderte Quellen und dieselbe Werkzeugkette
erzeugen dieselben Paketbytes.

## Was kann schiefgehen?

- **Ziel existiert:** Der Generator bricht ab und überschreibt keine Datei.
- **Ungültige ID, Paket oder Version:** Der Generator meldet die erwartete Form.
- **`javac` fehlt:** `JAVA_HOME` zeigt auf eine JRE; wählen Sie ein JDK 8.
- **SDK-Abhängigkeit fehlt:** Installieren Sie exakt den passenden Desktop-Stand
  lokal. Ändern Sie nicht stillschweigend die SDK-Version.
- **Paketprüfung schlägt fehl:** Vergleichen Sie Descriptor und Wurzelstruktur
  mit [Plugin SDK v1](plugin-sdk-v1.md).

## Sicherheits- und Betriebsgrenze

Build- und Testerfolg sind keine Sicherheitsfreigabe. Prüfen Sie generierten Code
und das Paket ohne Klassen zu laden. Installation und Aktivierung benötigen später
jeweils eine eigene bewusste Freigabe. Der Starter sendet keine Daten an Codex,
führt kein fremdes Plugin in der Desktop-JVM aus und verändert keine Datenbank.

Das SDK-Artefakt wird in AP 02 zunächst aus dem exakten Desktop-Source-Stand in
das lokale Maven-Repository installiert. Eine öffentliche Repository- oder
Signaturfreigabe ist Teil der unabhängigen Framework-/Distributionsgates.
