# Automatisiertes GitHub-Release und WordPress-Download

## Worum geht es?

Ein freigegebener Versionstag baut Companyware Desktop auf GitHub neu, erstellt
einen eigenständigen Windows-Installer mit privater Java-8-Laufzeit und
veröffentlicht Installer, portable ZIP, Prüfsummen und Release-Evidenz. Die
WordPress-Downloadseite benötigt dadurch keinen manuellen Dateiaustausch mehr.

Ein normaler Push auf `main` oder `develop` veröffentlicht keine Binärdatei. Nur
ein ausdrücklich freigegebener Tag `v<Version>` startet
`.github/workflows/release.yml`.

## Voraussetzungen

- Der freigegebene Commit ist auf GitHub `main` vorhanden.
- `pom.xml` enthält eine Non-SNAPSHOT-Version.
- `docs/release-notes/<Version>.md` ist vorhanden und freigegeben.
- Alle lokalen und externen Release-Gates sind bestanden.
- Der Tag lautet exakt `v<Version>`, beispielsweise `v0.29.0`.
- GitHub Actions besitzt die automatisch bereitgestellte Berechtigung
  `contents: write` für Releases.

## Veröffentlichung

Nach der dokumentierten Prüfung und ausdrücklichen Freigabe:

```powershell
git status --short
git rev-parse HEAD
git tag -a v<Version> -m "Companyware Desktop <Version>"
git push github v<Version>
```

Der Workflow führt danach folgende Schritte aus:

1. Er checkt den unveränderlichen Tag aus.
2. Er richtet Temurin Java 8 ein.
3. Er führt das vollständige Framework- und Test-Gate aus.
4. Er erstellt die portable Windows-Ausgabe.
5. Er erzeugt mit Inno Setup einen Per-User-Installer. Dieser installiert unter
   `%LOCALAPPDATA%\Programs\Companyware Desktop` und enthält seine private
   Java-Laufzeit.
6. Er verlangt, dass Tag und `pom.xml`-Version exakt übereinstimmen.
7. Er erzeugt stabile Asset-Namen und SHA-256-Prüfsummen.
8. Er veröffentlicht das GitHub-Release mit den geprüften Release Notes.

Versionen mit einem Bindestrich, etwa `0.29.0-rc.2`, werden automatisch als
Pre-release veröffentlicht. Nur ein normales veröffentlichtes Release wird über
GitHubs `latest`-Adresse an Endbenutzer ausgeliefert.

## Veröffentlichte Dateien

- `Companyware-Setup.exe`: eigenständiger Installer für normale Windows-Benutzer;
- `companyware-desktop.zip`: portable Ausgabe für kontrollierte Starts und
  Entwicklerprüfungen;
- `SHA256SUMS.txt`: Prüfsummen der veröffentlichten Dateien;
- `release.json`: Version, Tag, Commit, Zielgruppe und Prüfsummen;
- `release-evidence.json`: Evidenz aus dem reproduzierbaren Source-Build.

Buildausgaben werden nicht in Git eingecheckt.

## WordPress einmalig konfigurieren

Der Button **Download Setup.exe für Benutzer** erhält dauerhaft diese URL:

```text
https://github.com/Companyware/companyware-desktop/releases/latest/download/Companyware-Setup.exe
```

Der Button für die portable Ausgabe erhält:

```text
https://github.com/Companyware/companyware-desktop/releases/latest/download/companyware-desktop.zip
```

Die Links müssen nach späteren Releases nicht geändert werden. GitHub leitet sie
auf die gleichnamigen Dateien des neuesten normalen Releases um. Pre-releases
ändern diese Endbenutzerlinks bewusst nicht.

## Ergebnis kontrollieren

1. Öffnen Sie den Actions-Lauf **Publish Companyware Desktop release**.
2. Prüfen Sie, dass alle Schritte grün sind.
3. Öffnen Sie das erzeugte GitHub-Release und vergleichen Sie Tag, Version und
   Commit mit der Freigabe.
4. Laden Sie `Companyware-Setup.exe` über den WordPress-Link auf einen sauberen
   Windows-Testrechner herunter.
5. Vergleichen Sie den SHA-256-Wert mit `SHA256SUMS.txt`.
6. Installieren, starten, anmelden, abmelden, beenden und deinstallieren Sie die
   Anwendung. Prüfen Sie auch ein Upgrade mit einer disponiblen Testdatenbank.

## Fehler und sichere Abhilfe

- **Tag und Version stimmen nicht überein:** Der Workflow bricht vor der
  Veröffentlichung ab. Version und Release Notes korrigieren; einen bereits
  veröffentlichten Tag nicht verschieben.
- **Inno Setup fehlt auf dem Runner:** Nichts wird veröffentlicht. Erst den
  Runner beziehungsweise den expliziten Compilerpfad kontrolliert anpassen.
- **Build oder Tests schlagen fehl:** Kein Release erzeugen und Tests niemals
  überspringen.
- **`latest` liefert eine ältere Version:** Prüfen, ob das neue Release nur als
  Pre-release oder Draft veröffentlicht wurde. Das ist für Release Candidates
  beabsichtigt.
- **Windows warnt vor einem unbekannten Herausgeber:** Der Workflow erzeugt noch
  keine Authenticode-Signatur. Eine öffentliche stabile Version soll erst nach
  Einrichtung und Prüfung einer geschützten Codesignatur als vertrauenswürdiger
  Herausgeber beworben werden.
- **Installation oder Upgrade schlägt fehl:** Alte Release-Dateien unverändert
  lassen, die dokumentierte Datenbanksicherung verwenden und das Release nicht
  als aktuell bewerben.
