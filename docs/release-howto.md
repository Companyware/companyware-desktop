# Vom ersten Download zum Companyware-Desktop-Release

Diese Anleitung richtet sich an Maintainer, die Companyware Desktop auf einem
neuen Windows-Rechner zum ersten Mal herunterladen, prüfen und als GitHub
Pre-release veröffentlichen. Jeder Schritt nennt das erwartete Ergebnis. Bei
einer Abweichung nicht weitergehen, sondern zuerst den Hinweis unter
**Wenn es nicht klappt** verwenden.

Normale Benutzer benötigen diese Schritte nicht. Sie laden den eigenständigen
`Companyware-Setup.exe` über die Downloadseite und folgen dem Installer. Die
portable ZIP bleibt für kontrollierte Starts und Entwicklerprüfungen verfügbar.

Der vollständige Weg besteht aus zwölf Kontrollpunkten:

1. Werkzeuge installieren und prüfen.
2. Source-Code erstmals klonen.
3. JDK und Maven Wrapper prüfen.
4. Anwendung bauen und lokal starten.
5. Release-Version und Dokumentation abgleichen.
6. Framework- und Test-Gate ausführen.
7. Zwei unabhängige Clean-Clone-Builds vergleichen.
8. GitHub-Security- und Dependency-Ansichten prüfen.
9. Windows-ZIP wie ein Benutzer starten und sauber beenden.
10. Exakte Freigabe für Commit, Tag und Pre-release erteilen.
11. Tag pushen; der GitHub-Workflow baut Installer und Assets und veröffentlicht
    das Release automatisch.
12. Assets erneut herunterladen, Hash und Start prüfen und das Ergebnis dokumentieren.

Wer Codex zur Unterstützung verwenden möchte, folgt stattdessen dem ergänzenden
[Release-Leitfaden mit Codex](codex-release-howto.md). Die technischen Gates und
Abbruchregeln sind in beiden Wegen identisch.

## 1. Einmalige Vorbereitung

Benötigt werden:

- Git für Windows mit PowerShell;
- ein 64-Bit-JDK 8 mit `java.exe` und `javac.exe`;
- Schreibzugriff auf `Companyware/companyware-desktop` für die Veröffentlichung;
- ein neues Arbeitsverzeichnis, das keine produktive Companyware-Datenbank enthält.

Prüfen Sie Git und das JDK in PowerShell:

```powershell
git --version
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-8...'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

**Erwartet:** Git zeigt eine Versionsnummer; Java und Javac zeigen beide Java 8.
Der konkrete JDK-Ordner darf anders heißen.

**Wenn es nicht klappt:** Bei „Befehl nicht gefunden“ Git beziehungsweise das JDK
installieren und PowerShell neu öffnen. Existiert `javac` nicht, wurde nur eine
JRE statt eines JDK ausgewählt. Bei unterschiedlichen Java-Versionen `JAVA_HOME`
und den vorangestellten `Path` korrigieren.

## 2. Source-Code erstmals herunterladen

```powershell
Set-Location C:\Users\<Name>\Projects
git clone https://github.com/Companyware/companyware-desktop.git
Set-Location companyware-desktop
git status --short
git branch --show-current
```

**Erwartet:** `git status --short` gibt nichts aus; der aktuelle Branch ist
`main`. Dieser Ordner ist der Source-Code für Entwickler. JAR und ZIP werden erst
beim Build unter `target` erzeugt und nicht in Git eingecheckt.

**Wenn es nicht klappt:** Bei einem bereits vorhandenen Zielordner nicht darüber
klonen. Einen neuen Ordner wählen oder den vorhandenen Checkout bewusst mit
`git status` prüfen. Lokale Änderungen niemals durch Reset oder Löschen
übergehen.

## 3. Anwendung bauen und lokal starten

```powershell
.\mvnw.cmd clean verify
java -Dcompanyware.persistence.embedded.path=target\release-test-data\companyware `
  -jar target\Companyware.jar
```

**Erwartet:** Maven endet mit `BUILD SUCCESS`. Danach öffnet sich das
Companyware-Anmeldefenster. Bei einer frischen Testdatenbank lautet die erste
Anmeldung `companyware` / `companyware`; anschließend muss sofort ein neues
Passwort mit mindestens zehn Zeichen vergeben werden.

Beenden Sie Companyware über **Datei → Beenden** oder das Fenster-X.

**Wenn es nicht klappt:** Maven nennt den ersten fehlgeschlagenen Test. Nicht mit
`-DskipTests` fortfahren. Bei einer Datenbanksperre eine noch laufende Testinstanz
regulär schließen; keine Lockdateien löschen. Testdaten unter `target` niemals mit
Produktivdaten verwechseln.

## 4. Kandidat vor dem Release festlegen

Vor jedem Release müssen `pom.xml`, die Runtime-Version, Release-Notizen und der
Plugin-Starter dieselbe Non-SNAPSHOT-Version verwenden. Außerdem müssen README,
Benutzer- und Entwicklerhandbuch den Kandidaten korrekt beschreiben.

```powershell
git pull --ff-only
git status --short
git rev-parse HEAD
```

**Erwartet:** Der Pull läuft ohne Merge; der Arbeitsbaum ist leer. Notieren Sie
den Commit. Ein Release wird niemals aus einem schmutzigen Arbeitsbaum gebaut.

## 5. Vollständige Release-Gates ausführen

Auf Windows kann der vollständige lokale Framework-Build auch per Doppelklick auf
`build-windows-release.cmd` gestartet werden. Das Fenster bleibt für Ergebnis und
Fehlerhilfe geöffnet. Der Starter ruft dasselbe Gate auf und erzeugt keinen
abweichenden zweiten Buildweg.

```powershell
.\tools\verify-framework-release.ps1
.\tools\verify-clean-clone.ps1
```

**Erwartet:** Das erste Skript endet mit `Framework release verified`. Das zweite
baut zwei frische Clones und endet mit `Two-build clean-clone release verified`.
Die ausgegebenen SHA-256-Werte beider Builds müssen übereinstimmen.

Prüfen Sie anschließend in GitHub unter **Security** beziehungsweise
**Settings → Security & analysis**, dass keine offenen kritischen oder hohen
Dependency-, Code- oder Secret-Scanning-Funde vorliegen. Prüfen Sie außerdem den
aktuellen Actions-Lauf für den Release-Commit.

**Wenn es nicht klappt:** Keinen Tag anlegen und nichts hochladen. Ursache
beheben, Änderung committen und pushen, danach alle Gates auf dem neuen Commit
von vorn ausführen.

## 6. Windows-Paket wie ein Benutzer prüfen

Die fertigen Dateien liegen unter `target\release`. Entpacken Sie die dortige
`companyware-desktop-<Version>.zip` in einen neuen Testordner. Starten Sie darin
`start-companyware.cmd`, melden Sie sich an, melden Sie sich wieder ab und
beenden Sie das Programm regulär.

**Erwartet:** Start, Login, Passwortwechsel bei frischen Daten, Abmeldung und
Beenden funktionieren. Danach läuft kein zu diesem Test gehörender Java-Prozess
mehr; JAR, ZIP und Testordner lassen sich umbenennen. Das belegt, dass keine Datei
gesperrt blieb.

**Wenn es nicht klappt:** Release abbrechen. `logs\companyware.log` sichern, aber
vor Weitergabe auf Benutzernamen oder interne Adressen prüfen. Den Prozess nicht
einfach als bestandenen Test im Task-Manager beenden.

## 7. Tag erstellen und pushen

Erst nach allen bestandenen Gates und ausdrücklicher Veröffentlichungsfreigabe:

```powershell
git status --short
git rev-parse HEAD
git tag -a v<Version> -m "Companyware Desktop <Version>"
git push origin v<Version>
```

Beispiel: `v0.29.0-rc.2`. Der Tag muss auf genau dem Commit liegen, den
`target\release\release-evidence.json` nennt.

**Wenn es nicht klappt:** Einen falschen Tag nicht durch einen zweiten Upload
kaschieren. Veröffentlichung stoppen und den dokumentierten Korrekturweg mit
Vier-Augen-Prüfung verwenden. Bereits veröffentlichte Tags werden nicht still
verschoben.

## 8. Automatisches GitHub-Release kontrollieren

Der Tag-Push startet **Publish Companyware Desktop release**. Der Workflow baut
erneut, erzeugt den eigenständigen Installer mit privater Java-Laufzeit, bereitet
die festen Asset-Namen vor und legt das Release an. Versionen wie
`0.29.0-rc.2` werden automatisch als Pre-release markiert.

Kontrollieren Sie im Actions-Lauf und im Release:

- `Companyware-Setup.exe` für normale Benutzer;
- `companyware-desktop.zip` als portable Ausgabe;
- `SHA256SUMS.txt`, `release.json` und `release-evidence.json`;
- den exakten Tag, Commit und die freigegebenen Release Notes.

Keine JAR, ZIP oder EXE wird in den Git-Source eingecheckt. Der Installer und die
portable Ausgabe müssen nach dem Download separat unter Windows installiert bzw.
gestartet, beendet und auf freigegebene Dateihandles geprüft werden. Details und
die dauerhaften WordPress-Links stehen in
[`github-release-automation.md`](github-release-automation.md).

## 9. Veröffentlichung von außen nachprüfen

Laden Sie Installer, portable ZIP und Prüfsummendatei aus dem soeben
veröffentlichten GitHub-Release in einen neuen Ordner und prüfen Sie:

```powershell
Get-FileHash .\Companyware-Setup.exe -Algorithm SHA256
Get-FileHash .\companyware-desktop.zip -Algorithm SHA256
Get-Content .\SHA256SUMS.txt
```

**Erwartet:** Beide berechneten SHA-256-Werte stimmen mit ihrem jeweiligen Eintrag
in `SHA256SUMS.txt` überein. Installieren Sie den heruntergeladenen Installer und
starten Sie auch die portable Ausgabe einmal. Erst dann ist die Veröffentlichung
abgeschlossen. Release-URL, Tag, öffentlicher Commit und Hashes werden in den
Release-Unterlagen dieses Repositorys dokumentiert.

## 10. Klare Abbruchregel

Bei einem roten Test, abweichendem Hash, offenen kritischen/hohen Security-Fund,
fehlgeschlagenem Windows-Start oder unklarer Commit-Zuordnung wird nicht
veröffentlicht. Ein neuer Commit macht alle vorherigen Buildnachweise ungültig;
die Gates beginnen dann wieder bei Schritt 4. Diese Wiederholung ist Absicherung,
kein Fehler im Prozess.
