# Technische Bestandsaufnahme der Java-Desktopanwendung

- Stand der Analyse: 2026-10-06
- Analysierter Branch: `develop`
- Analysierter Commit: `3373a5c511fda9e3b6895cd402d5854048c5f846`
- Umfang: vollständiger getrackter Java-, Ressourcen-, Build-, Test- und Dokumentationsstand dieses Repositories
- Nicht betrachtet als Produktquelle: generierte Inhalte unter `target/`, lokale Derby-Daten unter `data/` und andere nicht getrackte Laufzeitdateien

Diese Bestandsaufnahme beschreibt ausschließlich den tatsächlich vorhandenen Code. Sie nimmt kein Refactoring vor und entwirft keine neue Architektur. Wo eine Aussage nicht durch Code oder einen ausführbaren Test belegt ist, ist sie als Einschränkung oder offene Entscheidung markiert.

## 1. Technologie-Stack

| Bereich | Tatsächlicher Stand |
|---|---|
| Java | Java 8; Maven-Compiler `source` und `target` sind `1.8`. Die Windows-Verpackung bringt optional eine Oracle-JRE `1.8.0_131` ein. |
| Build | Maven, Maven Wrapper 3.3.4 mit Maven 3.9.16 und Prüfsumme; ein Maven-JAR mit Spring-Boot-Repackage. Optionales Launch4j-Profil für `Companyware.exe`. |
| Spring Boot | Parent `2.1.9.RELEASE`; Boot startet den Anwendungskontext und verpackt das ausführbare JAR. Es gibt keinen eingebetteten Webserver und keine Boot-Webanwendung. |
| Spring Framework | `spring-context`, `spring-beans`, `spring-core` und `spring-security-core`, explizit `5.1.6.RELEASE`. Spring übernimmt Kontext/Beans; Plugins und viele Controller werden weiterhin durch den eigenen Pluginmanager konstruiert. |
| UI | Swing/AWT, nicht JavaFX. FlatLaf `1.0-rc3` und eine eigene semantische Theme-Schicht. Im Projekt gibt es keine JavaFX-Abhängigkeit und keinen JavaFX-Import. |
| Datenbank | Apache Derby `10.14.2.0`; wahlweise privat eingebettet oder als Client eines extern betriebenen Derby Network Servers. |
| ORM/Persistenz | Hibernate Core `5.2.2.Final`, JPA-Annotationen und `.hbm.xml` für vier ältere Kernmodelle; Fluent Hibernate `0.3.1`. Neue Plugins nutzen überwiegend JDBC über `PluginDatabase`. |
| PDF | OpenHTMLToPDF/PDFBox `1.0.10` für XHTML/CSS nach PDF; PDFBox wird außerdem für die eingebettete Seitenvorschau und PDF/A-3-Anhänge verwendet. |
| E-Rechnung | Eigene UBL-/CII-Erzeugung, defensive lokale Prüfung, PDF/A-3-Einbettung als `factur-x.xml`, Derby-Archiv und SHA-256-Integritätswert. Keine vollständige offizielle XSD-/Schematron-Prüfung. |
| Mail | JavaMail `javax.mail` `1.6.2`; SMTP mit optionaler Authentifizierung und STARTTLS-Prüfung. |
| Templates | Kontrolliertes XHTML/CSS mit eigener Tokenersetzung und Whitelist-Validierung; getrennte Klartextvorlage für Rechnungs-E-Mails. Kein allgemeines Template-Framework wie Thymeleaf/Freemarker. |
| Logging | Log4j 1.2.17 über Commons Logging 1.1.3; Datei `logs/companyware.log` und Konsole. Zusätzlich strukturierte Start-/Plugin-Diagnostik und Audit-Logging. |
| Tests | JUnit 4.12, Spring Test, echte Derby-Integrationsläufe, Swing-Komponententests, Architekturtests und Tests bösartiger Pluginpakete. Kein Mocking-Framework. |
| Weitere Bibliotheken | SnakeYAML 1.7, Commons IO 2.1, Commons Lang 3.12.0, Maven Artifact 3.0.3, Launch4j im optionalen Windows-Profil. |

`spring.datasource.initialization-mode` und eine JPA-HBM2DDL-Eigenschaft stehen noch in `application.properties`; die reale Hibernate-Konfiguration wird jedoch in `HibernateUtils` aufgebaut und setzt `hibernate.hbm2ddl.auto=update`. Plugin-Schemaänderungen laufen zusätzlich über die eigene, versionierte Migration Engine.

## 2. Projektstruktur

Das Projekt ist weiterhin ein einziges Maven-Artefakt. Die Trennung erfolgt durch Packages und wird teilweise durch `ArchitectureBoundariesTest` abgesichert.

| Package/Verzeichnis | Verantwortung |
|---|---|
| `Companyware.java`, `core.*` | Bootstrap, Spring-Kontext, Startfenster, Shutdown, ältere globale Container-Hilfen, Hibernate-Initialisierung, Diagnostik, Sicherheit und zentrale Lokalisierung. |
| `platform.api.*` | Öffentliches Plugin-SDK: Plugins/Deskriptoren, typisierte Services, Events, Persistenzgateway, Security, Lokalisierung, UI-Beiträge, Themes und anwendungsneutrale Resultate. |
| `pluginmanager.*` | Discovery, Deskriptorlesen, Dependency-Auflösung, Lifecycle, Service-/Action-Registries, externe Classloader sowie `.cwp`-Inspektion/Installation/Update/Deinstallation. |
| `persistence.*` | Auswahl Embedded/Client-Server, Derby-Verbindungsdaten, lokale Instanzsperre, JDBC-Ressourcen, Plugin-Datenbankgateway und Migrationen. |
| `desktop.ui.*`, `core.ui.*` | Swing-Registries, Theme-Auswahl/-Anwendung, EDT-Koordination und geschütztes helles Standardtheme. |
| `models.*` | Ältere Hibernate-Kernmodelle und Repositories für Benutzer, Plugins, globale Einstellungen und Plugin-Einstellungen. |
| `plugins.core.*` | Desktop-Rahmen, Login, Menü, Info, Pluginverwaltung und Benutzerverwaltung. Ein Teil nutzt noch Legacy-Pluginvertrag und stringbasierte Services. |
| `plugins.community.*` | Demo, Kundenverwaltung, HelloWorld, Dark Theme und Rechnungen. Die Referenzplugins verwenden öffentliche SDK-Verträge; das Rechnungsplugin trennt Domain, Application, Infrastructure und View. |
| `src/main/resources` | Anwendungskonfiguration, Hibernate/Logging, globale Übersetzungen, Deskriptor-Schema, Branding und ältere Setup-SQL-Dateien. |
| `docs` | Benutzer-, Entwickler-, Betriebs-, Release- und Architektur-Dokumentation. |
| `tools` | Releasebau, Pluginpaketbau, Clean-Clone-Verifikation und Startskript. |

### Bewertung der Schichtentrennung

Die neue Plattform- und Community-Plugin-Seite ist erkennbar getrennt: Views sprechen mit Application-Ports und DTOs; JDBC liegt in `infrastructure`; Rechnungsberechnung liegt in `domain`. Öffentliche SDK-Contracts stehen in `platform.api`.

Die Trennung ist noch nicht durchgehend sauber:

- `LoginController` greift neben typisierten Diensten weiterhin auf konkrete `FrameController`, `Frame`, `Menu` und das globale `ApplicationContextProvider` zu.
- `UserModel` enthält Swing-Felder, `DisplayAs`-Metadaten und einen Bezug auf die konkrete Benutzer-View. Das Persistenzmodell ist damit nicht UI-neutral.
- Die Kernverwaltung verwendet noch statische Hibernate-Repositories und Legacy-Plugininterfaces.
- `HibernateUtils` scannt ein physisches Verzeichnis (`src/main/java` beziehungsweise `classes`) nach Mappings; das ist eine Laufzeit-/Buildlayout-Kopplung.
- Das Maven-Modul erzwingt Grenzen nur über Tests, nicht über getrennte Artefakte oder JPMS.

## 3. Aktuell vorhandene Funktionen

| Funktion | Status | Codebeleg / Anmerkung |
|---|---|---|
| Anwendungsstart/-ende | vorhanden | `Companyware`, `StartProgressBar`, `ApplicationShutdown`; kontrollierter Spring-/Plugin-/Hibernate-Shutdown und lokale Derby-Instanzsperre. |
| Login | vorhanden | `LoginController`, `LoginView`, `DefaultIdentityService`; BCrypt, aktive Benutzer, Pflichtwechsel des Erstkennworts. |
| Benutzerverwaltung | vorhanden | `plugins.core.users.*`, `UserModel`, `HibernateUserAccountStore`; Anlegen/Bearbeiten, Aktivstatus, administrativer Passwortreset. |
| Profil | vorhanden | Profilansicht im Frame-/OutlookBar-Bereich; Name, E-Mail, eigenes Passwort, Sprache und Theme. |
| Rollen/Berechtigungen | teilweise vorhanden | Typisierte Identität, Rollen, Permissions und `AccessRequirement` existieren. Eine vollständige fachliche Rollenverwaltungsoberfläche ist nicht erkennbar. |
| Sprache/i18n | vorhanden | `DefaultLocalizationService`, `PluginMessages`, Auswahl in Login/Profil; Deutsch, Englisch, Französisch und Spanisch. Deutsch/Englisch werden auf Schlüsselkonsistenz getestet. |
| Theme-Auswahl | vorhanden | Geschütztes Light Theme und aktivierbares Dark-Theme-Plugin; Speicherung in OS-Preferences, Live-Umschaltung und Fallback. |
| Kunden | vorhanden | `Mbduscustomer`, `CustomerService`, `JdbcCustomerService`, `CustomerPanel`; Liste, Neu/Ändern/Löschen. |
| Kundenbezug in Rechnungen | teilweise vorhanden | Rechnungen speichern einen eigenen Kundennamen-/Adresssnapshot. Es gibt keine direkte Fremdschlüsselbeziehung oder Auswahl aus dem `Customer`-Plugin. |
| Rechnungsübersicht | vorhanden | `InvoiceOverviewPanel`, `InvoiceService.list()`, Status/Nummer/Kunde/Datum/Summen. |
| Rechnungserstellung | vorhanden | `InvoiceEditorPanel`, `CreateInvoiceCommand`, `JdbcInvoiceService`; Entwurf erstellen und ändern, Positionen und Summen. |
| Rechnungsfinalisierung | vorhanden | Nummernkreis pro Serie/Jahr; Nummernvergabe und Statuswechsel in derselben Transaktion. Finalisierte Rechnungen sind nicht editierbar. |
| Rechnungssuche | vorhanden | Eigene Suchansicht mit lokalem, spaltenübergreifendem `RowFilter` auf der geladenen Übersicht. Keine serverseitige/paginierte Suche. |
| PDF-Vorschau | vorhanden | `InvoiceDocumentService.previewPdf`, `PdfPagePreview`; echte PDF-Seiten für Entwürfe/finalisierte Belege und ungespeicherte Templateänderungen. |
| PDF-Export | vorhanden | Nur finalisierte Rechnungen; `JFileChooser` und explizite Überschreibbestätigung. |
| Rechnungsvorlagen | vorhanden | Persistiertes kontrolliertes XHTML/CSS, Tokenhilfe, Validierung, Default-Wiederherstellung und echte PDF-Vorschau. |
| Briefpapier | vorhanden | `InvoiceBrandingPanel`; Verkäuferstammdaten, Footer, PNG/JPEG-Logo, Breite/Ausrichtung und Defaultlogo. |
| E-Rechnung | teilweise vorhanden | XRechnung-UBL und ZUGFeRD-CII/PDF-A3, Archiv und Korrekturentwurf sind implementiert. Offizielle aktuelle XSD-/Schematron-Validierung fehlt und ist eine Freigabesperre. |
| Mailvorlagen | vorhanden | Persistierte Klartextvorlage für Betreff/Text mit kontrollierten Platzhaltern. |
| Mailversand | vorhanden | Reviewdialog, archiviertes PDF zwingend, XML optional, JavaMail/SMTP, Versandprotokoll. Produktiver Versand wurde außerhalb des Codes dokumentiert; im Code gibt es Transporttests. |
| SMTP-Einstellungen | vorhanden | Host, Port, Absendername/-adresse, Benutzername und STARTTLS werden persistiert; Passwort bleibt Session-/Secret-Provider-basiert. Verbindungstest sendet keine Mail. |
| Allgemeine Einstellungen | teilweise vorhanden | `Settings`, `PluginSettings`, Einstellungsmenü, SMTP und Profil/Theme. Ein einheitliches modernes Configuration-UI/API für alle Alt-Einstellungen ist nicht vollständig. |
| Pluginverwaltung | vorhanden | Anzeigen, Laufzeit-Aktivierung/-Deaktivierung, `.cwp` installieren, externe Plugins aktualisieren/deinstallieren unter Abhängigkeitsregeln. |
| Demoübersicht | vorhanden (Referenz/Demo) | `Companywaredemo`, `DemoOverviewService`, `DemoPanel`; eigene Tabelle und Beispieldaten. Fachlich bewusst Demo. |
| HelloWorld | vorhanden (SDK-Beispiel) | `Helloworld`, `GreetingService`, `HelloWorldPanel`; eigene Migration/Tabelle und datenbankgestützter Gruß. |
| Info/Hilfe | vorhanden | `plugins.core.info.*`; Infoansicht und Hyperlink-Komponenten. |
| Strukturierte Startdiagnostik | vorhanden | `StructuredLog`, `StartupDiagnostics`, Plugin-Health/Diagnostics. |
| Backup/Release-Werkzeuge | vorhanden | Dokumentierte Derby-Sicherung, reproduzierbarer Releasebau und Clean-Clone-Prüfung unter `tools/`. |

Nicht im Code vorhanden sind Buchhaltung, Kasse/Bank/Konten, Projektmanagement, Vereinsverwaltung, Termin-/Erinnerungssystem, WhatsApp oder Push-Benachrichtigungen.

## 4. Rechnungsmodul

### Aufbau

- Composition Root: `plugins.community.invoice.Invoice`
- Deskriptor: `Invoice.yaml`, ID `companyware.community.invoice`, Version `1.8.0`, Capability `companyware.invoice-management`
- Domain: `InvoiceLine`, `CalculatedInvoiceLine`, `InvoiceCalculator`, `InvoiceTotals`, `TaxSummary`
- Application-Ports: `InvoiceService`, `InvoiceDocumentService`, `EInvoiceService`, `InvoiceMailService`
- DTOs/Commands: `CreateInvoiceCommand`, `InvoiceDto`, `InvoiceDetailsDto`, `InvoiceLineInput`, `InvoiceLineDto`, `InvoiceTemplateDto`, `InvoiceBrandingDto`, `InvoicePdfDto`, `EInvoiceArchiveDto`, `InvoiceMail*`, `SmtpSettingsDto`
- Infrastruktur: `JdbcInvoiceService`, `JdbcInvoiceDocumentService`, `JdbcEInvoiceService`, `JdbcInvoiceMailService`, `InvoiceTemplateEngine`, `EInvoiceXmlGenerator`, `EInvoiceValidator`, `PdfA3InvoiceEmbedder`, `SmtpMailTransport`
- UI: `InvoiceWorkspacePanel`, Übersicht, Editor, PDF-Vorschau, Templateeditor, Briefpapier, Mailvorlage und SMTP-Einstellungen

### Tabellen

| Tabelle | Zweck |
|---|---|
| `CW_INVOICE` | Status, Nummer/Serie, Daten, Verkäufer- und Kundensnapshot, Zahlungsziel und Summen. |
| `CW_INVOICE_LINE` | Positionssnapshot mit Menge, Einzelpreis, Steuersatz und berechneten Beträgen. |
| `CW_INVOICE_SEQUENCE` | Nächster Wert je Serie und Kalenderjahr. |
| `CW_INVOICE_TEMPLATE` | Singleton für kontrolliertes XHTML/CSS. |
| `CW_INVOICE_BRANDING` | Singleton für Logo, Druckbreite/Ausrichtung, Verkäuferdefaults und Footer. |
| `CW_EINVOICE_ARCHIVE` | Unveränderliches Hybrid-PDF, XRechnung-XML, Dateiname, Hash und Archivzeitpunkt. |
| `CW_INVOICE_CORRECTION` | Verknüpfung Originalbeleg zu Korrekturentwurf und Begründung. |
| `CW_INVOICE_MAIL_TEMPLATE` | Singleton für Betreff- und Textvorlage. |
| `CW_INVOICE_MAIL_LOG` | Erfolgreicher Versand: Rechnung, Empfänger, Betreff, Zeit und XML-Anhang ja/nein. |
| `CW_INVOICE_SMTP_SETTINGS` | Persistierte nicht geheime SMTP-Werte. |

### Ablauf: neue Rechnung bis Mail/Export

1. `InvoiceEditorPanel` erzeugt einen `CreateInvoiceCommand`; neue Entwürfe werden optional mit den aktuellen Briefpapier-Verkäuferdefaults vorbelegt.
2. `JdbcInvoiceService.createDraft` validiert Pflichtwerte, rechnet Positionen mit `InvoiceCalculator` und speichert `CW_INVOICE` plus `CW_INVOICE_LINE` über `PluginDatabase.write` in einer Transaktion.
3. Der Entwurf kann über `updateDraft` geändert werden. Die Übersicht lädt unveränderliche DTOs; die Suche filtert die geladene Tabelle.
4. `finalizeDraft` validiert Entwurfsstatus, Serie und Datum. `CW_INVOICE_SEQUENCE` wird erhöht und die formatierte Rechnungsnummer zusammen mit dem Statuswechsel in derselben Transaktion gespeichert.
5. `JdbcInvoiceDocumentService` lädt den vollständigen Snapshot, validiert/ersetzt die erlaubten Template-Tokens und rendert XHTML/CSS mit OpenHTMLToPDF. Entwürfe dürfen intern als PDF vorab betrachtet werden; Datei-Export ist finalisierten Belegen vorbehalten.
6. Normaler PDF-Export schreibt die erzeugten Bytes ausschließlich an einen vom Benutzer über `JFileChooser` gewählten Pfad.
7. Für E-Rechnungen erzeugt `JdbcEInvoiceService` UBL und CII, prüft defensiv Kerndaten, bettet CII als `factur-x.xml` in das PDF ein und archiviert PDF/XML/Hash einmalig in Derby. Der Export schreibt PDF und `-xrechnung.xml` an den gewählten Ort.
8. `JdbcInvoiceMailService.draft` setzt die Mailvorlage ein. Nach dem modalen Review sendet `SmtpMailTransport` das bereits archivierte PDF und optional die XML-Datei. Erst nach erfolgreichem Transport entsteht ein Eintrag in `CW_INVOICE_MAIL_LOG`.

### Kunden, Einstellungen und Abhängigkeiten

- Die Rechnung übernimmt Kundendaten als Textsnapshot; sie kennt weder `CustomerDto` noch die Kundentabelle. Das schützt Altbelege, verhindert aber aktuell eine direkte Kundenauswahl.
- Templates, Briefpapier, E-Rechnung, SMTP und Mailvorlage gehören vollständig zum Rechnungsplugin.
- Das Plugin hängt laut Deskriptor nur von Frame und Menü ab; technisch bezieht es zusätzlich öffentliche Plattformdienste für Datenbank, DesktopShell und Secrets.
- PDF- und Archivdaten liegen nicht in einem allgemeinen Dokumentenmodul. Das Rechnungsplugin besitzt Speicherung und Lebenszyklus selbst.

## 5. Datenhaltung

### Datenbank und Ort

- Standard: Derby Embedded unter dem relativen Pfad `data/companyware`; JDBC-URL `jdbc:derby:data/companyware;create=true`.
- Alternative: externer Derby Network Server über `jdbc:derby://<host>:<port>/<database>;create=true`.
- Auswahl und Werte kommen aus `application.properties` und können durch JVM-Systemproperties `companyware.persistence.*` überschrieben werden.
- Embedded-Betrieb verwendet neben der Datenbank eine Prozesssperre `<datenbankname>.companyware.lock`.
- Die Standardzugangsdaten `app`/`ap` stehen im getrackten `application.properties`. Das ist Konfiguration, kein Benutzerlogin, bleibt aber für reale Network-Server-Bereitstellungen ein Risiko.

### Entities und Tabellen

Hibernate-Entities sind `PluginModel` (`Plugins`), `PluginSettingsModel` (`PluginSettings`), `SettingsModel` (`Settings`) und `UserModel` (`Users`). Plugin-JDBC-Tabellen sind `CW_SCHEMA_MIGRATION`, `Demo`, `Customer`, `HelloWorld` sowie die zehn oben genannten Rechnungstabellen. Security-Migrationen erweitern `Users` um Passwortwechselstatus, Rollen, Permissions und Locale.

### Dateien, PDFs, Templates und Konfigurationen

- Final erzeugte PDFs besitzen keinen festen Anwendungsordner. Normaler PDF-/XML-Export geht an den vom Benutzer gewählten Pfad.
- Unveränderliche E-Rechnungsartefakte werden als BLOBs in `CW_EINVOICE_ARCHIVE` gespeichert, nicht im Dateisystem.
- Die aktive Rechnungsvorlage, Mailvorlage und Briefpapierdaten liegen in Derby. Default-XHTML und Defaultlogo liegen als Classpath-Ressourcen im Rechnungsplugin.
- Eine nicht mehr verwendete HTML-Vorschauhilfe kann ein Logo temporär über `Files.createTempFile` materialisieren und löscht es wieder; die aktuelle PDF-Vorschau rendert Bytes direkt.
- Installierte externe Plugins liegen standardmäßig unter `plugins-installed/<plugin-id>`; überschreibbar durch `companyware.plugins.directory`.
- Logs liegen unter `logs/companyware.log`.
- Theme-Auswahl liegt in Java User Preferences und damit außerhalb von Derby.
- SMTP-Passwörter liegen nur im prozesslokalen `EnvironmentSecretProvider` oder in Umgebungs-/JVM-Secretwerten, nicht in der SMTP-Tabelle.

### Storage-Abstraktion und lokale Annahmen

`PluginDatabase` abstrahiert Transaktionen und das gewählte Derby-Profil. Für Dokumente gibt es keine plattformweite Storage-Abstraktion; PDF-Export, Pluginpakete, Logs, Preferences und temporäre Dateien nutzen direkt lokale Java-Dateisystem-/Desktopmechanismen. Lokale Pfade im Code sind insbesondere `data/companyware`, `plugins-installed`, `logs/companyware.log`, `src/main/java`, `classes`, `target/classes` und vom Benutzer gewählte Exportziele. Embedded Derby, Pluginpaketverwaltung, Logdatei, Preferences, Dateiauswahl und Release-/Startskripte setzen lokalen Speicher voraus.

## 6. Plugin-System

### Definition und Discovery

Ein modernes Plugin implementiert `platform.api.plugin.Plugin` und besitzt einen YAML-Deskriptor mit stabiler ID, Name, Autor, SemVer, erforderlicher API-Version, Entry Point, Priority, Capabilities und Dependencies. Das JSON-Schema liegt in `plugin-descriptor.schema.json`. Legacy-Plugins implementieren daneben noch `pluginmanager.plugininterfaces.Plugin`.

Built-ins werden aus den Pluginpackages/deskriptoren entdeckt. Externe Plugins werden aus vollständigen Verzeichnissen unter `plugins-installed` geladen, wenn `plugin.yaml`, `plugin.jar` und passende `.companyware-package.properties` mit `trust=trusted-local` vorhanden sind.

### Laden und Lifecycle

`PluginLifecycleEngine` löst Mindestversionen, fehlende Dependencies, Zyklen und deterministische Reihenfolge auf. Zustände umfassen entdeckt/installiert/aufgelöst/gestartet/gestoppt/fehlgeschlagen beziehungsweise uninstall-pending. Aktivierung und Deaktivierung erfolgen zur Laufzeit; Dependants blockieren das Stoppen. Startfehler rollen Registrierungen zurück, Stop läuft umgekehrt, Ressourcen schließen über `PluginContext.closeOnStop`.

Built-ins verwenden den prozessweiten Loader. Jedes aktive externe Paket erhält einen eigenen `PackagePluginClassLoader`, der nach Pluginressourcen beim Stop geschlossen wird. Das ist Lebenszyklus- und Namensraumtrennung, aber keine Sicherheitsisolation: Plugin-Code läuft mit vollen JVM-Rechten.

### Navigation, UI, Services, Datenbank und Konfiguration

- `UiRegistry` verwaltet ownergebundene Menüs, Navigation, Gruppen, Views, Dialoge und Settings-Beiträge auf dem EDT.
- `ThemeRegistry` verwaltet Basisthemes/Overlays mit geschütztem Light-Fallback.
- Typisierte Services nutzen `ServiceKey`; der stringbasierte Registry-Altweg existiert parallel.
- Plugins erhalten `PluginDatabase`, eigene versionierte Migrationen und transaktionsgebundene JDBC-Callbacks.
- Pluginlokale Übersetzungen kommen aus `messages*.properties`.
- `PluginConfiguration` und ältere XML-/`PluginSettings`-Mechanismen existieren parallel.
- Aktivstatus und Pluginmetadaten werden in Derby gespeichert.

### Installation, Update und Deinstallation

`.cwp` ist ein ZIP mit mindestens `plugin.yaml` und `plugin.jar`. Vor Codeausführung werden Pfade, Dubletten, Größen, Kompressionsverhältnisse, Deskriptor und API-Kompatibilität geprüft. Installation erfolgt über Staging, SHA-256-Metadaten und atomaren Tausch mit Rollback. Updates benötigen dieselbe Identität, höhere Version und inaktiven Zustand. Nur inaktive externe Leaf-Plugins können deinstalliert werden; Tabellen und Migrationshistorie bleiben absichtlich erhalten. Signaturen fehlen noch und sind vor Drittverteilung ausdrücklich erforderlich.

### Vorhandene Plugins

| ID | Typ | Zweck |
|---|---|---|
| `companyware.core.frame` | Core/Legacy | DesktopShell und OutlookBar |
| `companyware.core.login` | Core/Legacy | Login und Erstpasswortwechsel |
| `companyware.core.menu` | Core/Legacy | Datei-/Einstellungen-/Hilfe-Menüs |
| `companyware.core.info` | Core/Legacy | Info/Hilfe |
| `companyware.core.plugins` | Core/Legacy | Pluginverwaltung |
| `companyware.core.users` | Core/Legacy | Benutzerverwaltung |
| `companyware.theme.dark` | SDK | alternatives Dark Theme |
| `companyware.community.demo` | SDK | Datenbank-/UI-Referenzdemo |
| `companyware.community.mbduscustomer` | SDK | Kundenverwaltung |
| `companyware.community.helloworld` | SDK | kleinstes vollständiges SDK-Beispiel |
| `companyware.community.invoice` | SDK | Rechnungen, Dokumente, E-Rechnung und Mail |

### Eignung als zukünftiges Plugin-SDK

Bereits gut wiederverwendbar sind versionierte Deskriptoren, typisierte Services, ownergebundene Ressourcen, Dependency-/Lifecycle-Engine, UI-/Theme-Beiträge, Lokalisierung, Migrationen, `PluginDatabase`, sichere lokale Paketbehandlung und Compile-/Architekturtests. Vor einem öffentlich belastbaren SDK fehlen insbesondere signierte Distribution, stabile Kompatibilitäts-/Deprecationregeln über mehrere SDK-Versionen, Entfernung der Legacy-Registries aus Core, eine klarere Plugin-Konfigurations-API und echte Isolation für nicht vertrauenswürdigen Code.

## 7. API und Netzwerk

Es existiert keine fachliche HTTP- oder REST-Kommunikation: kein REST-Client, kein HTTP-Server, kein Webcontroller, kein OpenAPI-Vertrag, keine JSON-Bibliothek und keine Remote-Authentifizierung. `java.net` wird nur für Classloader/URI/Links und in Tests für Derby Network Server verwendet.

Vorhandene Netzwerkkommunikation ist:

- Derby Client zum externen Derby Network Server;
- JavaMail/SMTP für Rechnungsversand;
- Öffnen externer Hyperlinks über Desktop/URI;
- Maven-Abhängigkeitsauflösung zur Buildzeit.

API-Vorbereitung existiert auf Codeebene durch `ApplicationResult`, stabile Fehlerkategorien, unveränderliche DTOs und Application-Ports in Demo, Kunde, HelloWorld und Rechnung. Das ist transportneutral, aber noch keine Netzwerk-API.

## 8. Offline und Synchronisation

| Konzept | Stand |
|---|---|
| Offline-Betrieb | Embedded Derby ermöglicht vollständigen lokalen Betrieb. |
| Lokale/remote Daten | Umschaltbares Derby-Profil; fachlich bleibt es jeweils genau eine Datenbankverbindung. Ein gleichzeitiges Local-plus-Remote-Modell existiert nicht. |
| Synchronisation | nicht vorhanden |
| Fachliche UUIDs | nicht vorhanden; Primärschlüssel sind überwiegend Derby-Identity/Long. UUIDs werden nur für temporäre Paketverzeichnisse verwendet. |
| Änderungszeitpunkte | punktuell vorhanden (`CREATED_AT`, `FINALIZED_AT`, `UPDATED_AT`, `ARCHIVED_AT`, `SENT_AT`), aber nicht als allgemeines Sync-Protokoll. |
| Datensatzversionierung/Optimistic Locking | nicht vorhanden |
| Konflikterkennung/-auflösung | nicht vorhanden |
| Tombstones/Änderungsjournal | nicht vorhanden |
| Remote-Sync-Authentifizierung | nicht vorhanden |

Der derzeitige Client/Server-Modus ist gemeinsamer direkter Datenbankzugriff, keine Synchronisation und keine Companyware-API.

## 9. Tests

Der aktuelle Testquellstand enthält 105 Methoden mit `@Test` in 29 Testklassen; `sdkexample.ExamplePlugin` ist zusätzlich eine Compile-only-SDK-Probe. Die vorhandenen Surefire-Berichte dokumentieren 101 erfolgreich gelaufene Tests ohne Fehler. Ein neuer vollständiger Maven-Lauf am 2026-10-06 scheiterte vor der Kompilierung, weil die Shell nur eine Java-8-JRE ohne Compiler bereitstellt; dies ist kein Testfehler, aber die vier jüngsten Testmethoden konnten hier nicht als Gesamtsuite neu bestätigt werden.

| Bereich | Abdeckung |
|---|---|
| Architektur/Schichtengrenzen | Import-/Abhängigkeitsregeln, Ressourcenlebenszyklus, Übersetzungsbundles |
| Plugin-SDK/Lifecycle | SemVer, Deskriptoren, Services, Dependencies, Zyklen, Aktivierung, Rollback, Health |
| Paketinstallation | sichere Archive, Größen/Pfade, atomare Installation, Update/Uninstall, Classloader-Freigabe |
| Persistenz | Profile, Embedded-Sperre/-Probe, JDBC-Cleanup, Migrationen gegen Embedded und Network Server |
| Security/i18n | Loginlogik, Passwortwechsel, Secrets, Security-Schema, Locale/Fallback |
| UI/Themes | Registries, Dark/Light, OutlookBar-Gruppen/Icons, Rechnungs-Workspace-Komponenten |
| Rechnungen | Rundung, Template-Sicherheit, JDBC-Integration, Nummernkonkurrenz, PDF/E-Rechnung/Mailpfade, SMTP-Transport |

Besonders kritisch ohne ausreichende direkte Tests sind der vollständige reale Login-/Profil-/Benutzer-Swingablauf, Legacy-Menü und -Controller, Dateiexport mit realem `JFileChooser`, allgemeine End-to-End-Plugininstallation über die sichtbare Oberfläche, Restore aus Backup, Produktiv-Network-Server über längere Laufzeit und offizielle E-Rechnungsvalidierung. Es gibt keine automatisierten Robot-/Window-UI-Tests und keine API-/Sync-Tests, weil diese Funktionen nicht existieren.

## 10. Konkrete Architekturrisiken

1. **Zwei Persistenzstile teilen eine Datenbank.** Core arbeitet mit statischen Hibernate-Repositories/HBM2DDL, neue Plugins mit JDBC-Migrationen. Reihenfolge und Schemaeigentum sind dadurch schwieriger stabil zu halten.
2. **Legacy-UI koppelt Kernpfade an konkrete Klassen und String-Services.** `LoginController` castet `FrameController` und greift direkt auf `Menu`; das erschwert einen austauschbaren lokalen/remote Adapter und sichere Plugin-Deaktivierung.
3. **Benutzer-Entity enthält Swing.** `UserModel` besitzt `JButton`, `JCheckBox`, View-Metadaten und eine konkrete View-Abhängigkeit; ein server- oder API-neutraler Identitätskern kann dieses Modell nicht direkt übernehmen.
4. **Aktuelle Rechnungs-Kundendaten sind nicht mit dem Kundenmodul integriert.** Nur Textsnapshots werden gespeichert; für eine API muss geklärt werden, ob und wie ein stabiler Kundenbezug zusätzlich zum rechtlich nötigen Snapshot entsteht.
5. **Long-Identity-IDs sind nicht synchronisationsfähig definiert.** Ohne globale IDs, Versionen und Tombstones kollidieren lokal und serverseitig erzeugte Datensätze bei späterer Zusammenführung.
6. **Client/Server bedeutet heute direkten Derby-Zugriff.** Mehrere Desktops können dieselbe DB nutzen, aber dieser Pfad hat andere Sicherheits-, Versions- und Betriebsannahmen als die geplante Symfony-API.
7. **Dokumente besitzen keine allgemeine Storage-Grenze.** Rechnungsartefakte sind BLOBs oder benutzergewählte Dateien; lokal-only, LAN und Symfony-Storage lassen sich noch nicht über einen gemeinsamen Vertrag adressieren.
8. **Trusted Plugins sind nicht isoliert oder signiert.** Ein installiertes Plugin hat volle JVM-/Dateisystem-/Datenbankrechte; das begrenzt ein öffentliches Ökosystem und servernahe Zugangsdaten im Desktop.
9. **E-Rechnungsfreigabe ist unvollständig.** Eigene defensive Prüfung ersetzt keine aktuelle offizielle XSD-/Schematron-Prüfung; Versand kann formal ungültige Dokumente erzeugen.
10. **Veraltete Kernabhängigkeiten erhöhen Integrations- und Sicherheitsrisiko.** Java 8, Spring Boot 2.1, Hibernate 5.2, Log4j 1.2, SnakeYAML 1.7 und sehr alte Commons-Versionen schränken langfristige Wartung und moderne API-/TLS-Stacks ein. Dies ist aus `pom.xml` konkret belegt; eine Upgrade-Reihenfolge ist noch nicht entschieden.

## 11. Wiederverwendbare Bestandteile

- **Rechnungsdomain:** `InvoiceCalculator`, Snapshotmodell, transaktionale Serien-/Jahresnummern und Immutabilitätsregeln sollten fachlich erhalten bleiben.
- **Application-Ports/DTOs:** `InvoiceService`, `InvoiceDocumentService`, `EInvoiceService`, `InvoiceMailService` und `ApplicationResult` sind geeignete Adaptergrenzen.
- **PDF/Templates:** kontrollierte Token, Escaping, Whitelist-Validierung, deterministische PDF-Erzeugung und identische Preview-/Exportbytes sind wertvoll.
- **E-Rechnungsarchiv:** einmalige Archivierung mit Hash und Korrekturbezug ist eine gute fachliche Grundlage; die offizielle Validierung muss ergänzt werden.
- **Plugin-System:** Deskriptor, Dependency/Lifecycle, ownergebundene Registrierungen, Migrationen und sichere lokale Installation sind klare Plattformwerte.
- **UI-Komponenten:** `StandardView`, `CompanywareIcons`, `UiRegistry`, `ThemeRegistry` und PDF-Seitenvorschau sind wiederverwendbar.
- **Persistenzinfrastruktur:** `PersistenceSettings`, `PluginDatabase`, `MigrationRunner`, Embedded-Sperre und duale Derby-Profile sind für den Desktop wertvoll. Die Hibernate-Legacyschicht sollte nicht als Zielvertrag betrachtet werden.
- **Konfiguration/Security:** typisierte Security-/Secret-Verträge, OS-Preferences für nutzerspezifische Darstellung und persistierte nicht geheime SMTP-Werte sind klare Ausgangspunkte.

## 12. Erster API-Durchstich für Rechnungen (nur Betroffenheitsanalyse)

Wenn Rechnungen künftig optional über eine Symfony-API geladen werden, wären folgende bestehende Klassen/Verträge betroffen:

1. `InvoiceService` ist der zentrale Port für Liste, Detail, Entwurf, Update und Finalisierung. Ein neuer Remote-Adapter könnte denselben Port implementieren; die Swing-Views müssten grundsätzlich nicht geändert werden.
2. `JdbcInvoiceService` bleibt der lokale Adapter. Die Auswahl lokal/remote gehört an den Composition Root `Invoice.initialize`, nicht in die Views.
3. `InvoiceDto`, `InvoiceDetailsDto`, `CreateInvoiceCommand`, `InvoiceLineInput` und `ApplicationResult` bilden den ersten Mappingumfang. Für echte API-Stabilität benötigen sie noch explizite Transportversionen und globale IDs.
4. `InvoiceOverviewPanel`, `InvoiceEditorPanel` und `InvoiceWorkspacePanel` rufen bereits nur den Port auf. Sie benötigen höchstens UI-Behandlung für Latenz, Authentifizierung und `UNAVAILABLE`, nicht für HTTP selbst.
5. `InvoiceDocumentService` und `JdbcInvoiceDocumentService` müssen getrennt entschieden werden: lokale PDFs/Templates/Briefpapier können lokal bleiben, während Rechnungssnapshots remote kommen. Der heutige JDBC-Dokumentadapter erwartet lokale Rechnungsdaten über `InvoiceService`, ist aber zugleich für Template-/Brandingpersistenz an lokale Derbytabellen gebunden.
6. `EInvoiceService`/`JdbcEInvoiceService` und `InvoiceMailService`/`JdbcInvoiceMailService` hängen an Dokumenten, Archiv und Rechnungssnapshot. Vor dem Durchstich muss feststehen, ob Archivierung und Versand lokal oder serverseitig autoritativ sind.
7. `InvoiceServiceIds` und Pluginregistrierung müssen den gewählten Adapter veröffentlichen. Das Plugin-Lifecycle-System selbst benötigt keine Symfony-Kopplung.
8. `AuthenticationService`, `SecretProvider` und neue, noch nicht vorhandene API-Credential-/Tokenverträge wären für Remotezugriff erforderlich. Das bestehende lokale Login ist kein Symfony-Token.

Der kleinste fachlich sinnvolle Lese-Durchstich wäre daher: Übersicht und Detail über eine Remote-Implementierung von `InvoiceService`, während PDF-Vorschau/-Export zunächst lokal aus dem gelieferten unveränderlichen Snapshot erzeugt werden. Das ist eine Betroffenheitsbeschreibung, keine Implementierungsentscheidung.

## 13. Offene Architekturentscheidungen vor Java–Symfony

1. Welche Daten besitzt Desktop, Symfony oder beide autoritativ – getrennt pro Modul und Dokumentart?
2. Wird zwischen lokalem Modus, direktem Derby-Mehrbenutzermodus und API-Modus gewählt, oder dürfen sie gleichzeitig aktiv sein?
3. Welche globalen IDs, Versionsfelder, Änderungsjournale und Löschmarker werden für spätere Synchronisation eingeführt?
4. Wie werden Konflikte erkannt und wer darf sie fachlich auflösen?
5. Welche API-Versionierungs-, Fehler-, Paging-, Filter- und Idempotenzregeln gelten?
6. Wie werden Desktopbenutzer mit Symfony-Identitäten verbunden und Tokens/Secrets sicher gespeichert/erneuert?
7. Wo liegen Rechnungs-PDF, XRechnung, Anhänge und große lokale Dokumente; wie werden nur Metadaten beziehungsweise Verweise synchronisiert?
8. Wo werden Rechnung finalisiert, Nummern vergeben und unveränderliche Artefakte archiviert, wenn mehrere Systeme beteiligt sind?
9. Wie werden Plugin-/Modulversionen zwischen Desktop und Server auf Kompatibilität geprüft, ohne ihre Laufzeitsysteme zu koppeln?
10. Bleibt Derby Client/Server langfristig ein unterstützter Betriebsmodus neben der API, und wenn ja, wie wird sein Schema-Lifecycle vom API-Schema getrennt?
11. Welche Teile des Plugin-SDK werden öffentlich stabil garantiert, und wie werden Legacy-Plugins migriert?
12. Welche aktuelle E-Rechnungsnorm und welcher offizielle Validator sind verpflichtender Pre-Send-/Release-Gate?

## 14. Zusammenfassung

| Bereich | aktueller Stand | Reifegrad | wichtigste Klassen | Handlungsbedarf |
|---|---|---:|---|---|
| Bootstrap/Build | reproduzierbarer Maven-/Eclipse-Stand, Java 8, Swing | B | `Companyware`, `ApplicationShutdown`, `pom.xml` | JDK-Verfügbarkeit und spätere Runtime-/Dependency-Strategie klären |
| Desktop-UI | funktionsfähige Swing-Shell, Themes, Beiträge, Icons | B | `Frame`, `JOutlookBar`, `UiRegistry`, `StandardView` | Legacy-Controllerkopplungen abbauen |
| Login/Benutzer/Profil | funktionsfähig mit BCrypt, Erstwechsel, Locale/Theme | B | `DefaultIdentityService`, `LoginController`, `UserModel` | Entity/UI-Kopplung und vollständige Rollenverwaltung klären |
| Lokalisierung | Laufzeitwechsel, vier Sprachen, Pluginbundles | B | `DefaultLocalizationService`, `PluginMessages` | weitere Sprachen nur geprüft freigeben |
| Kunden | CRUD als SDK-Referenzplugin | B | `CustomerService`, `JdbcCustomerService`, `CustomerPanel` | stabilen optionalen Rechnungsbezug definieren |
| Rechnungsdomain/-workflow | Entwurf, Positionen, Summen, Finalisierung, Suche | B | `InvoiceService`, `JdbcInvoiceService`, `InvoiceCalculator` | globale IDs/API-Ownership/Sync festlegen |
| PDF/Templates/Briefpapier | kontrolliert, PDF-genau, lokal exportierbar | B | `InvoiceDocumentService`, `JdbcInvoiceDocumentService`, `InvoiceTemplateEngine` | Storage-Grenze für lokal/LAN/Server definieren |
| E-Rechnung | Generierung, Hybrid-PDF, Archiv, Korrektur | C | `JdbcEInvoiceService`, `EInvoiceXmlGenerator`, `PdfA3InvoiceEmbedder` | offiziellen XSD-/Schematron-Gate ergänzen |
| Mail | Vorlage, Review, SMTP, Anhänge, Erfolglog | B | `InvoiceMailService`, `JdbcInvoiceMailService`, `SmtpMailTransport` | Betriebs-/Secretmodell für API-Modus entscheiden |
| Persistenz | Derby embedded/client-server, Hibernate-Core + Plugin-JDBC | B | `PersistenceSettings`, `PluginDatabase`, `MigrationRunner`, `HibernateUtils` | zwei Persistenzstile und direkte DB-Fernnutzung strategisch klären |
| Plugin-SDK/Lifecycle | echtes SDK, Live-Aktivierung, sichere lokale Pakete | B | `PluginContext`, `PluginLifecycleEngine`, `PluginManagerImpl` | Signaturen, Kompatibilität, Legacy-Abbau, Isolation |
| API/Netzwerk | transportneutrale Ports, aber keine Fach-HTTP-API | D | `ApplicationResult`, Plugin-Application-Ports | API-Vertrag, Auth und Adapter erst entscheiden |
| Offline/Sync | lokaler Betrieb ja, Synchronisation nein | D | `PersistenceSettings` | IDs, Versionen, Journal, Konflikte und Ownership entwerfen |
| Tests | breite Unit-/Integrations-/Architekturabdeckung; keine vollständigen UI-E2E-Tests | B | `src/test/java/**` | aktuellen 105-Test-Stand mit JDK vollständig ausführen; kritische E2E-Lücken schließen |

### Gesamturteil

Companyware Desktop ist kein bloßer Legacy-Mock-up mehr. Der aktuelle Stand enthält eine belastbare lokale Plattformbasis, ein substanzielles Plugin-SDK und ein funktional breites Rechnungsreferenzmodul. Für die geplante gleichberechtigte Desktop-/Symfony-Landschaft sind die Application-Ports ein guter Ansatzpunkt. Nicht vorhanden sind jedoch die eigentliche Companyware-HTTP-API, fachliche Synchronisation, globale Identitäten/Versionen, Konfliktbehandlung und eine allgemeine Storage-Grenze. Diese Entscheidungen müssen vor einem produktiven Java–Symfony-Durchstich getroffen werden.
