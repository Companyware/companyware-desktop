# Companyware-Dokumentation

Diese Dokumentation beschreibt **Companyware Desktop**, die eigenständig
veröffentlichte Java-Anwendung der Produktfamilie Companyware. **Companyware Web**
ist eine separate Symfony-Anwendung mit eigenem Pluginformat, Build, Tests und
Release-Status. Eine Verbindung entsteht ausschließlich über versionierte APIs.

Die Dokumentation ist die erste Anlaufstelle für Fragen zu Companyware Desktop. Sie ist
nach Zielgruppe und Aufgabe gegliedert, damit niemand zuerst die interne Architektur
kennen muss.

## Erste Schritte für Benutzer

- [Benutzerhandbuch](benutzerhandbuch.md): Start, Anmeldung und Bedienung
- [Erstkennwort ändern](benutzerhandbuch.md#erstkennwort-ändern)
- [Eigenes Profil und Passwort](benutzerhandbuch.md#eigenes-profil)
- [Abmelden, Benutzer wechseln und Zugang wiederherstellen](benutzerhandbuch.md#abmelden-und-zugang-wiederherstellen)
- [Sprache auswählen und ändern](benutzerhandbuch.md#2-anmelden)
- [Plugins verwalten](benutzerhandbuch.md#5-plugins-verwalten)
- [Lokales Plugin installieren, aktualisieren oder deinstallieren](benutzerhandbuch.md#lokales-plugin-installieren)
- [Rechnungsgrundlage und Nummernvergabe](benutzerhandbuch.md#rechnungen)
- [Rechnungsvorlage, Vorschau und PDF](benutzerhandbuch.md#rechnungsvorlage-und-pdf)
- [Firmenlogo im Briefpapier](benutzerhandbuch.md#firmenlogo-im-briefpapier)
- [SMTP-Zugang für Rechnungen einrichten](benutzerhandbuch.md#smtp-zugang-einrichten)
- [Häufige Probleme](benutzerhandbuch.md#8-häufige-probleme)

## Installation und Betrieb

- [Privater Embedded-Betrieb](benutzerhandbuch.md#privater-betrieb)
- [Firmenbetrieb mit Derby Network Server](benutzerhandbuch.md#firmenbetrieb)
- [Datensicherung und Wiederherstellung](derby-backup.md)
- [Sichere Betreiber-Recovery](derby-backup.md#zugangsdaten-und-recovery-schluessel)
- [SMTP für Kontowiederherstellung](derby-backup.md#smtp-für-e-mail-bestätigung-und-kontowiederherstellung)
- [Qualität, Releases und Rollback](release-process.md)
- [Dependency- und Security-Review 0.29.0-rc.2](security-review-0.29.0-rc.2.md)
- [Security-Meldungen und unterstützte Versionen](../SECURITY.md)
- [Support und sichere Fehlerberichte](../SUPPORT.md)

## Entwicklung und Erweiterungen

- [Technische Bestandsaufnahme der Java-Desktopanwendung](current-state-java.md): belegter Ist-Stand, Funktionen, Datenhaltung, Plugin-System und API-Betroffenheit
- [Entwicklerhandbuch](developer-guide.md): Entwicklungsumgebung, Architektur und Tests
- [Plugin SDK v1](plugin-sdk-v1.md): öffentliche Pluginverträge und Descriptor
- [Eigenständiges Pluginprojekt erzeugen](standalone-plugin-starter.md):
  Java-8-Starter, versionsgebundenes SDK und reproduzierbares `.cwp`
- [Desktop-Framework veröffentlichen](framework-release.md): öffentlicher Umfang, Kompatibilität, Lizenz- und Release-Gates
- [Unveröffentlichte Release-Notizen](release-notes/UNRELEASED.md): vorbereitete Evidenz und noch offene Gates
- [Release Candidate 0.29.0-rc.2](release-notes/0.29.0-rc.2.md): aktueller unveröffentlichter GitHub-Kandidat
- [GitHub-Publikationscheckliste](github-publication-checklist.md): exakte lokale, externe und Freigabe-Gates
- [GitHub-Repository-Umzug](github-repository-migration.md): Trennung von historischem Repository, Desktop und Web
- [Mitwirken](../CONTRIBUTING.md): Build, Pull Requests und Desktop/Web-Grenze
- [Referenzplugins](reference-plugins.md): HelloWorld, Demo, Kundenverwaltung und Paketbau
- [Rechnungsplugin entwickeln](developer-guide.md#rechnungsplugin)
- [Ein Plugin erstellen](developer-guide.md#plugin-boundaries)
- [Security-Verträge](developer-guide.md#security-contracts)
- [Themes und Swing](developer-guide.md#themes-and-swing)
- [Übersetzungen und 28-Sprachen-Plan](localization.md)

## Dokumentationsstandard

- [So dokumentieren wir Companyware](documentation-standard.md)

Neue oder geänderte Funktionen gelten erst dann als vollständig, wenn die passende
Benutzer- und/oder Entwicklerdokumentation aktualisiert wurde.
