# Companyware Desktop mit Codex veröffentlichen

Dieser Leitfaden beschreibt denselben sicheren Zwölf-Punkte-Prozess wie
[`release-howto.md`](release-howto.md), verwendet Codex aber als ausführenden und
dokumentierenden Assistenten. Er ersetzt weder die Tests noch die Freigabe durch
den Maintainer.

## 1. Neuen Rechner vorbereiten

Installieren Sie Git für Windows, Codex und ein 64-Bit-JDK 8. Öffnen Sie Codex im
gewünschten Projektordner und geben Sie ein:

> Prüfe Git, Java und Javac. Klone anschließend
> `https://github.com/Companyware/companyware-desktop.git`, falls der Checkout
> noch nicht existiert. Verändere oder lösche keine vorhandenen Dateien.

Codex soll die erkannten Versionen und den absoluten Checkout-Pfad nennen.

### Eigenständigen Workspace auf einem neuen Notebook klonen

Für Entwicklung und Release wird nur das öffentliche Desktop-Repository benötigt:

```powershell
New-Item -ItemType Directory -Force C:\Users\<Name>\Projects\companyware
Set-Location C:\Users\<Name>\Projects\companyware
git clone https://github.com/Companyware/companyware-desktop.git
Set-Location companyware-desktop
```

Öffnen Sie anschließend den geklonten Ordner `companyware-desktop` als Workspace
in Codex und übergeben Sie diesen kopierfertigen Startkontext:

> Projekt: Companyware Desktop. Dieser Checkout ist vollständig und benötigt
> kein weiteres Repository. Lies zuerst `AGENTS.md`, `README.md` und
> `docs/README.md`. Prüfe den Arbeitsbaum, verwirf keine vorhandenen Änderungen
> und führe Pulls nur mit `--ff-only` aus. Aktuelles Ziel: Companyware Desktop
> als geprüften GitHub Pre-release
> vorbereiten. Vor Tag oder Veröffentlichung alle Repository-, Clean-Clone-,
> Plugin-Starter-, Windows-Start/Stop- und Security-Gates ausführen. Stoppe vor
> Tag und Release und lege mir Commit, Tree, Version, Hashes sowie bestandene und
> offene Prüfungen zur ausdrücklichen Freigabe vor. Dokumentiere alle für die
> Übergabe notwendigen Informationen im Repository und pushe den Source-Stand,
> bevor auf einem anderen Notebook weitergearbeitet wird.

Codex soll als erste Antwort Commit-ID, aktuellen Branch, Arbeitsbaumstatus,
erkannte JDK-Version und die nächsten offenen Gates zusammenfassen. Erst wenn
diese Angaben plausibel sind, beginnt die eigentliche Arbeit.

## 2. Projektkontext laden

Geben Sie im geöffneten `companyware-desktop`-Workspace ein:

> Lies `AGENTS.md`, `README.md`, `docs/README.md`, die aktuelle Release-Notiz und
> beide Release-Leitfäden vollständig. Prüfe den Arbeitsbaum. Bei lokalen
> Änderungen sofort stoppen und sie nur berichten. Ziehe einen sauberen Branch
> ausschließlich mit `--ff-only`.

Codex soll die exakte Commit-ID nennen. Auf einem anderen Notebook wird der
Kontext durch denselben Git-Commit und die Repository-Dokumentation hergestellt.

## 3. Ersten Build durchführen

> Baue Companyware Desktop mit dem vorgesehenen JDK 8 und
> `.\mvnw.cmd clean verify`. Berichte Testanzahl und Ergebnis. Überspringe keine
> Tests und ändere noch nichts.

Erwartet wird `BUILD SUCCESS`. Bei einem Fehler soll Codex Ursache und betroffene
Datei erklären, aber nicht ungefragt eine fachliche Änderung vornehmen.

## 4. Entwicklungsversion isoliert starten

> Starte `target\Companyware.jar` mit einer neuen Datenbank unter `target`, prüfe
> Anmeldefenster und Erstlogin und beende die Anwendung regulär. Verwende niemals
> bestehende oder produktive Daten.

Codex darf für die sichtbare Windows-Prüfung Computersteuerung verwenden. Das
Fenster muss regulär geschlossen werden; ein erzwungen beendeter Java-Prozess ist
kein bestandener Shutdown-Test.

## 5. Kandidat und Dokumentation prüfen

> Vergleiche Maven-, Runtime-, Starter- und Release-Notiz-Version. Prüfe README,
> Benutzerhandbuch, Entwicklerhandbuch, Download-, Installations- und
> Release-Anleitung aus Sicht eines Erstbenutzers. Liste erst Lücken auf und
> korrigiere nur belegte Lücken.

Nach Änderungen soll Codex Diff und Commit nennen. Jeder neue Commit macht ältere
Release-Evidenz ungültig.

## 6. Framework-Gate ausführen

> Führe auf dem sauberen finalen Commit
> `.\tools\verify-framework-release.ps1` mit JDK 8 aus. Stoppe bei jedem Fehler.

Codex soll Commit, Version, Testresultat und erzeugte Artefakte berichten.

## 7. Reproduzierbarkeit prüfen

> Führe `.\tools\verify-clean-clone.ps1` aus und bestätige, dass JAR, ZIP,
> Dependency-Inventar und `release-evidence.json` in zwei frischen Clones
> byteidentisch sind. Nenne die SHA-256-Werte.

Ein einzelner erfolgreicher Build reicht nicht.

## 8. Security und Dependencies prüfen

> Prüfe das lokale Dependency-Gate sowie die GitHub-Ansichten für Dependabot,
> Code Scanning, Secret Scanning, Advisories und Actions am exakten öffentlichen
> Commit. Veröffentliche bei einem offenen kritischen oder hohen Fund nichts.

Wenn eine GitHub-Funktion nicht aktiviert oder nicht zugänglich ist, muss Codex
das als offene Prüfung nennen und darf nicht „bestanden“ behaupten.

## 9. Fertiges Windows-ZIP prüfen

> Entpacke das erzeugte Windows-ZIP in einen neuen temporären Ordner. Starte
> `start-companyware.cmd`, prüfe Start, frischen Erstlogin, Passwortwechsel,
> Abmeldung und reguläres Beenden. Prüfe danach, dass JAR und Testordner nicht
> gesperrt sind. Keine Produktivdaten verwenden.

Codex soll den sichtbaren Endzustand und die verwendete Artefakt-Prüfsumme nennen.

## 10. Freigabepunkt

Codex muss nun stoppen und eine kompakte Freigabevorlage liefern:

- öffentlicher Commit und Tree;
- Version und Tag;
- bestandene Gates;
- offene oder nicht prüfbare Punkte;
- JAR- und ZIP-SHA-256;
- geplante Release-Assets;
- Hinweis, dass keine EXE veröffentlicht wird, solange ihr separates Gate fehlt.

Erst danach geben Sie ausdrücklich ein:

> Ich gebe genau diesen Commit für Tag `v<Version>` und den GitHub Pre-release
> frei. Veröffentliche ausschließlich die geprüften Assets.

Ohne diesen Satz erstellt Codex keinen Tag und kein Release.

## 11. Tag und Pre-release veröffentlichen

> Erstelle den annotierten Tag auf dem geprüften öffentlichen Commit, pushe ihn,
> lege einen GitHub Pre-release an und lade ausschließlich ZIP,
> ZIP-Prüfsummendatei und `release-evidence.json` hoch. Keine Binärdatei in die
> Git-Historie committen.

Codex soll danach Release-URL, Tag, Commit und die sichtbaren Assetnamen nennen.

## 12. Veröffentlichung unabhängig nachprüfen und übergeben

> Lade die drei Assets aus dem veröffentlichten Release erneut in einen frischen
> Ordner. Vergleiche den ZIP-Hash, entpacke und starte das heruntergeladene Paket
> einmal. Dokumentiere anschließend URL, Commit, Tag, Hashes und Testergebnis in
> den vorgesehenen Release-Unterlagen des Repositorys und pushe den finalen
> Source-Stand für die Notebook-Übergabe.

Erst wenn auch dieser Downloadtest bestanden und der Repository-Stand sauber
gepusht ist, ist die Aufgabe beendet.

## Universelle Abbruchanweisung für Codex

Diese Zusatzanweisung kann bei jedem Release-Auftrag mitgegeben werden:

> Bei schmutzigem Arbeitsbaum, abweichendem Commit/Tree, rotem Test,
> unterschiedlichem Reproduzierbarkeitshash, offenem kritischen/hohen
> Security-Fund, fehlgeschlagenem Windows-Start oder unklarer Freigabe sofort
> stoppen. Nichts löschen, keinen Test überspringen, keinen Tag verschieben und
> nichts veröffentlichen. Ursache und sicheren nächsten Schritt berichten.
