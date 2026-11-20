# Qualität, Releases und Rollback

## Entwicklungs-Gates auf Gitolite

`origin/develop` ist die Entwicklungsautorität. Eine Änderung darf erst gepusht
werden, wenn `mvnw.cmd clean verify` erfolgreich ist, der Arbeitsbaum keine
unbeabsichtigten Dateien enthält und betroffene Betriebs- und Entwicklerdokumente
aktualisiert wurden. Die Tests enthalten insbesondere Architektur-, Plugin-
Lifecycle-, schädliche Paket- und beide Derby-Profilprüfungen.

Die zusammengefasste F01-Prüfung lautet:

```powershell
.\tools\verify-framework-release.ps1
```

Sie prüft zuerst Lizenzheader und verpflichtende Framework-Dokumente und ruft
anschließend den bestehenden reproduzierbaren Releasebau auf. `-PolicyOnly` ist
nur eine lokale Vorprüfung für Rechner ohne JDK und kein Veröffentlichungsnachweis.
Solange `pom.xml` eine `-SNAPSHOT`-Version enthält, verweigert das vollständige
Veröffentlichungs-Gate bewusst die Freigabe. `-AllowSnapshot` bleibt ausschließlich
für kurzlebige Entwicklungsprüfungen verfügbar; die vorbereitete GitHub-CI und
alle Release-Candidate-Artefakte verlangen eine explizite Non-SNAPSHOT-Version.

## Reproduzierbares Release

Auf Windows mit einem JDK in `JAVA_HOME`:

```powershell
.\tools\build-release.ps1
```

Das Skript führt das vollständige Maven-Gate aus und erzeugt unter
`target/release` das ausführbare `Companyware.jar`, die Launch4j-`Companyware.exe`, deren SHA-256-Dateien sowie
ein versionsspezifisches Archiv wie `companyware-desktop-0.29.0-rc.2.zip` und
dessen SHA-256-Datei. Das Archiv enthält außerdem MIT-Lizenz,
Security-/Support-Hinweise, das exakte Dependency-Inventar und den Commit in
`BUILD-COMMIT`. `release-evidence.json` verknüpft Version, Commit und alle
Prüfsummen maschinenlesbar. Sortierte, unkomprimierte Archiveinträge, ein
festgelegter Build-Zeitstempel und der Maven-Wrapper machen den Build für
denselben Commit bytegleich wiederholbar. Der strengere Nachweis aus einem
frischen lokalen Clone lautet:

```powershell
.\tools\verify-clean-clone.ps1
```

Der Clean-Clone-Nachweis baut denselben Commit in zwei unabhängigen lokalen Clones
und vergleicht JAR, ZIP, Dependency-Inventar und Release-Evidence. Erst identische
Ausgaben gelten als Reproduzierbarkeitsevidenz.

Die Startdiagnose schreibt `event=application.started` und je Plugin ein
`event=plugin.health` als einzeilige Schlüssel/Wert-Ereignisse nach
`logs/companyware.log`. Passwörter und Datenbank-URLs werden dabei nicht erfasst.

## Veröffentlichung nach GitHub

Gitolite `develop` bleibt die Entwicklungsautorität. Das öffentliche Ziel ist
`Companyware/companyware-desktop`; das historische Repository
`Companyware/companyware` ist kein Veröffentlichungsziel für neue Versionen.
GitHub `main` wird nicht
automatisch gespiegelt. Ein ausdrücklich geprüfter Entwicklungsstand darf als
Quellcodebasis nach `main` übernommen werden, muss dort aber sichtbar
**Development preview** bleiben. Das veröffentlicht noch keinen Release.

Für einen bewusst gewählten Release-Commit wird zuerst das Clean-Clone-Gate
ausgeführt, die Prüfsumme protokolliert und ein signiertes Release-Tag auf genau
diesem Commit angelegt. Erst danach werden Tag und unveränderliche Artefakte
explizit veröffentlicht. Lokale Daten und ungeprüfte Buildausgaben werden nie
übertragen.

## Upgrade und Rollback

Vor jedem Upgrade wird die Derby-Datenbank gemäß `derby-backup.md` konsistent
gesichert und die aktuelle JAR samt Prüfsumme aufgehoben. Danach wird Companyware
vollständig beendet und das neue Paket eingespielt. Bei einem Fehler wird zuerst
das neue Programm beendet, anschließend die vorherige Programmversion und – falls
Migrationen liefen – ausschließlich die dazugehörige Sicherung gemeinsam
wiederhergestellt. Eine alte JAR darf nie gegen eine bereits vorwärts migrierte
Datenbank gestartet werden.

Nach einem regulären Beenden müssen die Embedded-Datenbank und installierte
Plugin-JARs ohne manuellen Prozessabbruch verschiebbar oder ersetzbar sein. Bleibt
eine Datei unter Windows gesperrt, kein Derby- oder Plugin-Lockfile löschen:
zuerst im Task-Manager prüfen, ob noch ein Companyware-Java-Prozess läuft, dessen
Protokoll sichern und den Austausch abbrechen. Release- und Plugin-Dateien werden
erst ersetzt, wenn der Prozess vollständig beendet ist.

Ein live installiertes Paket liegt standardmäßig unter
`plugins-installed/<plugin-id>/plugin.jar`. Es wird erst beim Aktivieren durch
einen eigenen Classloader geöffnet. Updates und Deinstallationen ausschließlich
über den Pluginmanager ausführen: Er verlangt vorherige Deaktivierung, prüft
Version und Abhängigkeiten und koordiniert Paketverzeichnis, Lifecycle und
Metadaten mit Rollback. Die Datenbanksicherung bleibt erforderlich, weil
Pluginmigrationen und Fachtabellen bei einer Deinstallation absichtlich nicht
gelöscht werden. Ein aktiviertes Paket niemals direkt überschreiben.

## Rechnungsdaten sichern und wiederherstellen

Das Rechnungsplugin hält Entwürfe, festgeschriebene Snapshots, Positionen, Vorlage,
Briefpapier-Firmendaten, Footer und Logo sowie
Nummernkreise in derselben Derby-Datenbank wie die übrigen Pluginmigrationen.
Deshalb muss vor einem Upgrade oder einer Wiederherstellung immer die vollständige
Datenbank konsistent gesichert werden; einzelne `CW_INVOICE*`-Tabellen dürfen nicht
separat zurückkopiert werden.

Beim Restore gehören `CW_INVOICE`, `CW_INVOICE_LINE`, `CW_INVOICE_SEQUENCE`,
`CW_INVOICE_TEMPLATE` und `CW_SCHEMA_MIGRATION` zwingend zum selben
Sicherungszeitpunkt. Andernfalls könnten
vergebene Rechnungsnummern erneut verwendet oder Migrationen fälschlich als
ausgeführt behandelt werden. Das Plugin löscht Rechnungsdaten bei Deaktivierung
oder Deinstallation nicht. Nach einem Restore muss vor neuer Rechnungsstellung
geprüft werden, ob inzwischen außerhalb der Sicherung Rechnungen festgeschrieben
wurden.

Das Festschreiben über die Rechnungsoberfläche wird unmittelbar transaktional
gespeichert. Nach der angezeigten Rechnungsnummer darf ein Backup daher nicht auf
einen älteren Stand einzelner Rechnungstabellen zurückgesetzt werden; es gilt immer
die gemeinsame Wiederherstellung der vollständigen Datenbank.

Die editierbare XHTML/CSS-Vorlage wird mit der Datenbank gesichert. Manuell
exportierte Kopien liegen zusätzlich am gewählten Dateisystemort; maßgeblich ist
für über Companyware ausgegebene E-Rechnungen das unveränderliche lokale Archiv.
Die Sicherung umfasst außerdem `CW_EINVOICE_ARCHIVE` und
`CW_INVOICE_CORRECTION`. Archivierte PDF/XML-BLOBs und ihre SHA-256-Prüfsumme
müssen gemeinsam wiederhergestellt werden; einzelne Archivzeilen dürfen nicht
ersetzt oder neu generiert werden.

## SMTP für Rechnungen konfigurieren

SMTP-Server, Port, Absender, Benutzername und STARTTLS werden normalerweise unter
**Einstellungen → E-Mail-Versand** gepflegt und mit der Derby-Datenbank gesichert.
Ein dort eingegebenes Passwort bleibt ausschließlich für die aktuelle Sitzung im
Arbeitsspeicher und muss nach einem Neustart erneut eingegeben werden.

Für unbeaufsichtigte oder zentral verwaltete Installationen bleiben JVM-Properties
als Startvorgaben verfügbar, solange noch keine Datenbankeinstellung gespeichert ist:

```text
-Dcompanyware.invoice.smtp.host=smtp.example.org
-Dcompanyware.invoice.smtp.port=587
-Dcompanyware.invoice.smtp.from=rechnung@example.org
-Dcompanyware.invoice.smtp.starttls=true
```

Das Passwort liest der Plattform-Secret-Provider beispielsweise aus
`CW_SECRET_INVOICE_SMTP_PASSWORD`. Der bisherige Benutzername
`CW_SECRET_INVOICE_SMTP_USERNAME` wird als Startvorgabe übernommen und kann danach
in der Oberfläche gespeichert werden. Fehlt der Benutzername, wird ein bewusst
konfigurierter SMTP-Relay ohne Anmeldung verwendet. Passwörter gehören niemals in
`application.properties`, Datenbank, Mailvorlage oder Supportprotokoll. Vor
Produktion sind TLS-Zertifikat,
Absenderberechtigung, Testempfänger und die offiziellen E-Rechnungsprüfungen zu
verifizieren.
