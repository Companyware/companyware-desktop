# Referenzplugins

Die mitgelieferten Community-Plugins sind ausführbare Beispiele für das
Plugin SDK v1. Sie verwenden ausschließlich `platform.api.*`, Swing und die
Java-Standardbibliothek. Direkte Zugriffe auf `core`, `models`, `pluginmanager`
oder Implementierungen anderer Plugins sind durch Architekturtests verboten.

## Companywaredemo

`companyware.community.demo` zeigt die kleinste vollständige Erweiterung:

1. Datenbankschema über versionierte `PluginMigration`-Objekte anlegen,
2. lokalisierte Menü-, View- und Navigation-Contributions registrieren,
3. die Swing-Ansicht über die OutlookBar öffnen und
4. Daten über `PluginDatabase` lesen.

## Mbduscustomer

`companyware.community.mbduscustomer` zeigt eine einfache CRUD-Erweiterung.
Die Kundenansicht liest und schreibt ausschließlich über `PluginDatabase`.
Damit funktioniert derselbe Plugin-Code im Embedded- und im Serverprofil.
Das Plugin registriert **Kunden** über denselben öffentlichen View- und
Navigation-Vertrag in der OutlookBar und zusätzlich im Menü **Einstellungen**.

## HelloWorld

`companyware.community.helloworld` ist das kleinste vollständige Anwendungsplugin.
Es besitzt die Tabelle `HelloWorld`, registriert links das Ziel **HelloWorld** und
zeigt nach einem Klick rechts den grünen Text „Hello World!“. Es demonstriert die
öffentlichen View- und Navigation-Beiträge ohne interne Frame-Abhängigkeit.

## Companyware Dark Theme

`companyware.theme.dark` ist ein vollständiges Theme-Plugin ohne Fachlogik. Es
registriert ausschließlich einen öffentlichen `ThemeProvider`; die Desktop-Shell
kennt weder seine Klasse noch seine Farbwerte. Nach Aktivierung im Pluginmanager
steht es in der Profilauswahl zur Verfügung.

## Rechnungen

Das Rechnungsplugin demonstriert das öffentliche `CompanywareIcons`-Set:
**Rechnungen** bleibt als Hauptpunkt eine Textzeile, während die sechs
untergeordneten Arbeitsabläufe semantische, themeabhängige Java2D-Symbole mit
lokalisierten Beschriftungen verwenden. Neue Plugins wählen dasselbe Symbol anhand
der Bedeutung, statt eigene Rastergrafiken oder Core-Implementierungen zu kopieren.
Fehlt bei einem gruppierten Unterpunkt ein Icon, stellt die Shell automatisch das
neutrale Dokument-Symbol bereit.

`companyware.community.invoice` zeigt eine geschichtete Fachdomäne mit
transaktionaler Persistenz und einer lokalisierten Swing-Oberfläche. Das Plugin
registriert `InvoiceService`, eine gruppierte Navigation für Übersicht,
Neuerstellung, Vorlage und Suche sowie einen
Entwurfseditor, speichert vollständige Snapshots, berechnet Positions- und
Steuerbeträge und vergibt endgültige jahresbezogene Rechnungsnummern atomar beim
Festschreiben. Eine kontrollierte XHTML/CSS-Vorlage liefert Vorschau und
reproduzierbare PDF-Ausgabe. Die Views kennen nur Commands, DTOs und die beiden
Anwendungsinterfaces. Das lokale Briefpapier verwaltet Firmendaten, Footer und ein
geprüftes PNG/JPEG-Logo, das über `{{company.logo}}` ohne externen Zugriff eingebettet
wird. Eine frische Installation startet mit dem mitgelieferten mdbus-Logo und dem
geschäftspapiernahen Standardlayout; gespeicherte Briefpapier- und Vorlagendaten
haben weiterhin Vorrang. Vorlagen sind
niemals Quelle für Beträge oder Nummern.
Das zusätzliche `EInvoiceService` erzeugt ein unveränderlich archiviertes
PDF/A-3-Hybriddokument mit eingebettetem ZUGFeRD-CII, eine separate
XRechnung-UBL-Datei und verknüpfte Korrekturbelege.
`InvoiceMailService` ergänzt eine persistente Klartext-Mailvorlage, einen pro
Rechnung prüfbaren Draft und einen SMTP-Transport mit archiviertem PDF sowie
optionalem XRechnung-Anhang. Nicht geheime SMTP-Werte werden über die zugehörige
Einstellungsansicht gespeichert; Passwörter verbleiben im Secret-Provider und
werden bei einer Eingabe in der Oberfläche nur für die laufende Sitzung gehalten.
Anzeigename und Absenderadresse sind getrennte SMTP-Felder und werden erst im
JavaMail-Adapter als UTF-8-Absender kombiniert. Das gebündelte mdbus-Logo kann in
der Briefpapieransicht gezielt wiederhergestellt werden, ohne Firmendaten oder
Footer zu überschreiben.

## Installationspakete bauen

Nach `mvnw.cmd clean compile` werden installierbare `.cwp`-Dateien erzeugt:

```powershell
.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/companywaredemo `
  -Descriptor src/main/java/plugins/community/companywaredemo/Companywaredemo.yaml `
  -Output target/plugins/companywaredemo.cwp

.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/mbduscustomer `
  -Descriptor src/main/java/plugins/community/mbduscustomer/Mbduscustomer.yaml `
  -Output target/plugins/mbduscustomer.cwp

.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/helloworld `
  -Descriptor src/main/java/plugins/community/helloworld/Helloworld.yaml `
  -Output target/plugins/helloworld.cwp

.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/darktheme `
  -Descriptor src/main/java/plugins/community/darktheme/Darktheme.yaml `
  -Output target/plugins/darktheme.cwp

.\tools\build-plugin-package.ps1 `
  -PluginDirectory plugins/community/invoice `
  -Descriptor src/main/java/plugins/community/invoice/Invoice.yaml `
  -Output target/plugins/invoice.cwp
```

Jedes Paket enthält am Wurzelverzeichnis `plugin.yaml` und `plugin.jar` und kann
im Pluginmanager über „Plugin installieren“ ausgewählt werden.

## Vorlage für ein neues Plugin

Für ein neues Plugin genügen zunächst eine Descriptor-Datei, eine Klasse mit
`Plugin` und ein englisches `messages_en.properties`. Benötigt es eine Ansicht,
registriert es einen Menübeitrag und öffnet dort eine eigene Swing-Komponente.
Datenbankänderungen erhalten unveränderliche, aufsteigend versionierte Migrationen.
Die Referenzplugins dürfen als Kopiervorlage verwendet werden.

Sie folgen derselben kleinen Grundstruktur: Einstiegsklasse, Descriptor und
Übersetzungen liegen im Plugin-Wurzelpaket, Swing-Oberflächen im Ordner `view` und
SQL-Migrationen im Ordner `migration`. Leere Architekturordner werden nicht
angelegt. Neue Verantwortlichkeiten erhalten erst dann eigene sprechende Ordner,
wenn das Plugin sie tatsächlich benötigt.
