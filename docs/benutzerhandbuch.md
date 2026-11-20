# Companyware Desktop – Benutzerhandbuch

Dieses Handbuch beschreibt **Companyware Desktop**, die lokale Java-Anwendung der
Produktfamilie Companyware. Companyware Web ist eine separate Webanwendung und
nicht Bestandteil dieses Programms.
Einzelne Fachfunktionen werden durch Plugins bereitgestellt und können je nach
Installation abweichen.

## 1. Programm starten

Companyware kann privat mit einer eingebetteten Datenbank oder in einer kleinen
Firma mit einem zentralen Derby-Datenbankserver betrieben werden. Die Betriebsart
wird durch die Installation vorgegeben und muss im Alltag nicht ausgewählt werden.

Nach dem Start erscheint zunächst die Ladeanzeige. Wenn alle Kernmodule vorbereitet
sind, öffnet sich die Anwendung mit dem Anmeldefenster.

## 2. Anmelden

Bei einer neuen Desktop-Installation mit frischer Datenbank lautet der
Benutzername des vorinstallierten Administrators `companyware`; das Erstkennwort
lautet ebenfalls `companyware`. Diese Angaben gelten nur für die erste Anmeldung.
Eine bestehende Installation verwendet das bereits geänderte Kennwort.

1. Oben im Anmeldefenster die gewünschte Sprache auswählen.
   Das bereits sichtbare Hauptfenster mit Menü und Navigation wechselt sofort mit.
2. Benutzername eingeben.
3. Passwort eingeben.
4. **Login** wählen oder die Eingabetaste drücken.

Die Auswahl wechselt das Anmeldefenster sofort und wird nach erfolgreicher
Anmeldung für diesen Benutzer gespeichert. Wurde beim nächsten Start keine neue
Sprache gewählt, verwendet Companyware die gespeicherte Benutzersprache.

Mit **Passwort anzeigen** kann die Eingabe kurz kontrolliert werden. Verwende diese
Funktion nicht, wenn andere Personen den Bildschirm sehen können.

Bei falschen Anmeldedaten zeigt Companyware nur eine allgemeine Fehlermeldung. Damit
wird nicht verraten, ob ein bestimmter Benutzername existiert.

### Erstkennwort ändern

Der vorinstallierte Administrator und neu angelegte Benutzer müssen ihr
Erstkennwort bei der ersten erfolgreichen Anmeldung ändern:

1. Aktuelles Passwort erneut eingeben.
2. Ein neues Passwort mit mindestens zehn Zeichen festlegen.
3. Das neue Passwort wiederholen.

Das neue Passwort muss sich vom bisherigen unterscheiden; das gilt auch dann, wenn
das bisherige Passwort bereits mindestens zehn Zeichen lang ist. Erst nach erfolgreicher
Änderung wird die Anmeldung abgeschlossen. Das Erstkennwort sollte nicht weiter
verwendet oder notiert werden.

Companyware Web ist eine separate Anwendung und übernimmt diese Desktop-
Zugangsdaten nicht. Web-Administratoren werden dort ausdrücklich mit einem selbst
gewählten Passwort angelegt.

## 3. Hauptfenster

Oben befindet sich die Menüleiste. Links zeigt die Navigation die verfügbaren
Arbeitsbereiche. Der große rechte Bereich enthält die jeweils ausgewählte Ansicht.

Welche Menüs und Bereiche sichtbar sind, hängt von installierten Plugins sowie von
den Rollen und Berechtigungen des angemeldeten Benutzers ab. Ein Klick auf einen
Navigationseintrag öffnet die zugehörige Ansicht. Der aktive Eintrag bleibt mit
violettem Rand und farbigem Hintergrund markiert. So ist auch nach einem
Ansichtswechsel erkennbar, wo man sich befindet.

### Eigenes Profil

**Profil** befindet sich dauerhaft unten links. Dort kann der angemeldete Benutzer
seinen Namen und seine E-Mail-Adresse ändern. Für eine Passwortänderung müssen das
aktuelle Passwort sowie das neue Passwort zweimal eingegeben werden. Bleiben alle
drei Passwortfelder leer, werden nur die Profildaten gespeichert.

Das neue Passwort muss mindestens zehn Zeichen lang sein. Das bekannte
Erstkennwort `companyware` darf nicht erneut verwendet werden.

Im Profil kann außerdem die Sprache geändert werden. Menü einschließlich der
Plugin-Einträge, linke Navigation und Profilansicht wechseln sofort; ein Neustart
ist nicht erforderlich. Der aktuelle
Stand enthält qualitätsgesicherte deutsche und englische Texte sowie französische
und spanische Sprachpakete. Fehlt einem Plugin eine Übersetzung, zeigt Companyware
automatisch den englischen Text statt eines leeren Feldes.

### Helle oder dunkle Darstellung wählen

Das helle Companyware-Theme ist immer verfügbar. Für die dunkle Darstellung muss
ein Administrator zuerst im **Pluginmanager** das Plugin **Companyware Dark Theme**
aktivieren. Danach kann jeder Benutzer im **Profil** unter **Darstellung** zwischen
**Companyware Light** und **Companyware Dark** wählen. Die offene Anwendung wechselt
sofort; ein Neustart ist nicht erforderlich. Die Auswahl wird auf diesem Arbeitsplatz
gespeichert und beim nächsten Start erneut verwendet.

Wird das Dark-Theme-Plugin deaktiviert oder fehlt es, fällt Companyware automatisch
auf das geschützte helle Theme zurück. Die Anwendung bleibt dadurch bedienbar. Nach
einer erneuten Aktivierung wird eine zuvor gespeicherte dunkle Auswahl wieder
hergestellt.

Die Aktivierung im Pluginmanager ist gegen doppelte Oberflächenereignisse geschützt.
Auch wenn der sofortige Theme-Wechsel die Tabelle neu zeichnet, wird das Theme-Plugin
nur einmal gestartet. Die Tabellenbearbeitung wird dabei zuerst sauber abgeschlossen;
die sichtbare Umschaltung folgt unmittelbar danach.

Im dunklen Theme erscheinen Menü- und Untermenütexte hell auf dunkler Fläche;
auch der ausgewählte Eintrag bleibt kontrastreich lesbar. **Datei**, **Einstellungen**
und **Hilfe** verwenden dabei dieselbe dunkle Popup-Darstellung – unabhängig davon,
ob ein Menü vor oder erst nach dem Theme-Wechsel zum ersten Mal geöffnet wird.
Der Rückwechsel zu **Companyware Light** aktualisiert auch bereits geöffnete Ansichten,
Eingabefelder und Arbeitsflächen vollständig; ein Neustart ist nicht erforderlich.

Der primäre **Login**-Button zeigt in der hellen Darstellung dunkelblaue Schrift auf
der blauen Primärfläche. Der sekundäre Button **Abbrechen** bleibt dunkel auf heller Fläche.

## 4. Benutzer verwalten

Benutzerverwaltung ist nur für entsprechend berechtigte Benutzer vorgesehen.
Die aufgeklappten Unterpunkte **Benutzer hinzufügen** und **Benutzer** verwenden
dieselben violetten Symbolkacheln wie andere Companyware-Bereiche. In den Tabellen
stehen der einheitliche Stift für **Bearbeiten** und der rote Papierkorb für
**Löschen**; die gleichen Aktionen sehen auch im Pluginmanager identisch aus.

### Benutzer anlegen

1. In der linken Navigation **Benutzer hinzufügen** wählen.
2. Name, E-Mail-Adresse, Benutzername und Erstkennwort eintragen.
3. **Aktiv** auswählen, wenn die Anmeldung sofort möglich sein soll.
4. **Benutzer speichern** wählen.

Der neue Benutzer muss das vergebene Erstkennwort bei seiner ersten Anmeldung
ersetzen.

Die beiden zusammengehörigen Eingabebereiche stehen als kompakter Formularblock
mittig im Arbeitsbereich. Die Überschrift beginnt auf derselben Höhe wie in den
Tabellenübersichten.

### Benutzer bearbeiten

1. **Benutzer** öffnen.
2. Beim gewünschten Eintrag das Bearbeiten-Symbol wählen.
3. Daten ändern und speichern.

Ein deaktivierter Benutzer kann sich nicht anmelden. Benutzer sollten bevorzugt
deaktiviert statt vorschnell gelöscht werden, wenn ihre Daten noch benötigt werden.

Wird bei der Bearbeitung ein neues Passwort gesetzt, gilt es als temporäres
Erstkennwort und muss bei der nächsten Anmeldung geändert werden. Das bekannte
Erstkennwort `companyware` wird auch hier abgelehnt.

## 5. Plugins verwalten

Die mitgelieferten Plugins „Companywaredemo“, „Mbduscustomer“ und „HelloWorld“ sind
Referenzmodule. Nach ihrer Aktivierung erscheinen ihre lokalisierten Einträge im
Menü **Einstellungen** und im linken Navigationsbereich; beim Deaktivieren
verschwinden sie an beiden Stellen sofort wieder.
Alle normalen Navigationsziele stehen gemeinsam oben unter dem Companyware-Logo.
**Profil** bleibt als persönliche Aktion getrennt am unteren Rand.
„Companywaredemo“ zeigt Beispieldaten;
„Mbduscustomer“ stellt eine einfache Kundenverwaltung bereit. „HelloWorld“ legt
beim ersten Start seine eigene Tabelle an. Ein Klick auf **Demoübersicht**,
**Kunden** oder **HelloWorld** öffnet rechts die jeweilige Ansicht; HelloWorld zeigt
dort den grünen Text „Hello World!“.

Plugins erweitern Companyware um neue Arbeitsbereiche und Fachfunktionen.
Die mitgelieferten Ansichten verwenden ein gemeinsames Raster: Überschrift,
Tabellen und Formulare beginnen unabhängig vom verantwortlichen Plugin auf einer
einheitlichen Höhe. Der Beginn der Arbeitsfläche ist mit dem ersten Eintrag der
linken Navigation ausgerichtet. Einzelne spezialisierte Vollbildbereiche können bewusst davon
abweichen.

### Lokales Plugin installieren

1. **Einstellungen → Pluginmanager** öffnen.
2. **Plugin installieren** wählen.
3. Eine Datei mit der Endung `.cwp` auswählen.
4. Den Vertrauenshinweis lesen und nur bestätigen, wenn Quelle und Ersteller bekannt
   und vertrauenswürdig sind.

Companyware prüft Paketstruktur, Größe, Kompatibilität und Prüfsumme vor der
Installation. Das neue Plugin erscheint danach ohne Neustart im Pluginmanager und
ist zunächst ausgeschaltet. Über das Kontrollkästchen wird es sofort gestartet
oder gestoppt. Kann ein Plugin
nicht gestartet werden oder benötigt es ein anderes aktives Plugin, bleibt der
bisherige Zustand erhalten und Companyware zeigt den Grund an. Installierte Plugins
laufen innerhalb der Anwendung und sind keine isolierten Programme.

### Lokales Plugin aktualisieren oder deinstallieren

Für ein Update zuerst das Plugin über sein Kontrollkästchen deaktivieren. Danach
über **Plugin installieren** ein vertrauenswürdiges `.cwp` mit derselben Plugin-ID,
demselben Namen und einer höheren Versionsnummer auswählen. Companyware ersetzt
Paket, Lifecycle-Definition und Metadaten gemeinsam. Schlägt ein Schritt fehl,
bleibt die bisherige Version erhalten. Anschließend das Plugin wieder aktivieren.

Zum Deinstallieren das Plugin ebenfalls zuerst deaktivieren und danach in seiner
Tabellenzeile den Löschknopf wählen. Den Warnhinweis bestätigen. Ein Plugin kann
nicht entfernt werden, solange ein anderes installiertes Plugin davon abhängt.
Mitgelieferte Core- und Community-Plugins können nicht auf diesem Weg entfernt
oder durch ein Paket überschrieben werden. Datenbanktabellen und Migrationseinträge
des Plugins bleiben als Schutz vor Datenverlust erhalten.

## 6. Private und gemeinsame Datenbank

### Companyware beenden

Beenden Sie Companyware über **Datei → Beenden** oder das Fenster-X. Beide Wege
schließen die aktiven Plugins, die Datenbankverbindung und die lokale
Instanzsperre. Dabei werden auch offene Plugin-Dateien und interne Leseströme
freigegeben. Sobald das Programmfenster geschlossen ist, kann Companyware erneut
gestartet werden. Beenden Sie den Java-Prozess in Eclipse nur dann manuell, wenn
die Anwendung wegen eines externen Fehlers nicht mehr reagiert.

### Privater Betrieb

Im Standardbetrieb liegt die eingebettete Derby-Datenbank lokal im Verzeichnis
`data/companyware`. Es ist kein Datenbankserver erforderlich. Das Verzeichnis darf
während der Programmausführung nicht kopiert oder von einem zweiten
Companyware-Prozess geöffnet werden.

Companyware prüft dies bereits vor dem Datenbankstart. Läuft dieselbe eingebettete
Datenbank schon in einer anderen Companyware-Instanz, erscheint eine verständliche
Meldung und die zweite Instanz wird beendet. Zuerst die bereits laufende Anwendung
schließen; keine Derby-Sperrdateien löschen.

### Firmenbetrieb

Im Firmenbetrieb verbinden sich mehrere Arbeitsplätze mit einem separat betriebenen
Derby Network Server. Serveradresse, Port, Datenbank und Zugangsdaten werden durch
Administration oder Installation festgelegt. Companyware startet diesen Server
nicht selbst.

Wenn der Server nicht erreichbar ist, Netzwerkverbindung, VPN, Serverstatus und die
konfigurierte Adresse prüfen oder die zuständige Administration kontaktieren.

## Rechnungen

Das Plugin **Rechnungen** speichert den Verkäufer, den Rechnungsempfänger,
das Leistungsdatum, die Währung, Zahlungsbedingungen und alle Positionen als
eigenständigen Snapshot. Spätere Änderungen an Kundenstammdaten verändern diesen
Snapshot nicht.

Das Plugin wird im **Pluginmanager** zunächst inaktiv angezeigt. Beim Aktivieren
legt es seine Tabellen über die versionierte Pluginmigration an. Links erscheint
**Rechnungen** als eigener OutlookBar-Eintrag. Erst ein Klick darauf zeigt die
eingerückten Ziele, darunter **Übersicht**, **Neue Rechnung**, **Vorlage
bearbeiten**, **Briefpapier**, **Suche** und **Mailvorlage**. Nachfolgende
Hauptpunkte wie **Einstellungen** rutschen dabei unter diese Unterpunkte.
Die Rechnungsfunktionen erscheinen im einheitlichen Companyware-Iconstil mit ihrer
Beschriftung darunter; der Hauptpunkt **Rechnungen** selbst bleibt eine reine
Textzeile.

### Rechnungsentwurf erfassen und bearbeiten

1. Unter **Rechnungen** den Eintrag **Neue Rechnung** wählen.
2. Verkäufer- und Empfängerdaten, Leistungsdatum, Währung und Zahlungsbedingungen
   eingeben.
3. Mit **Position hinzufügen** Leistungen erfassen; Menge, Netto-Einzelpreis und
   Steuersatz werden als Dezimalzahlen eingegeben. Nicht benötigte Zeilen können
   markiert und entfernt werden.
4. **Entwurf speichern** wählen. Pflichtfelder, Datum und Zahlen werden vor dem
   Speichern geprüft.
5. Einen vorhandenen Entwurf unter **Übersicht** markieren und **Entwurf
   bearbeiten** wählen, um seinen Snapshot zu ändern.

Zum gezielten Auffinden einer Rechnung unter **Rechnungen** den Eintrag **Suche**
wählen. Das dort angezeigte Suchfeld erhält sofort den Eingabefokus und filtert
über alle sichtbaren Spalten. Die normale Übersicht bleibt frei von diesem Feld
und bietet mehr Platz für Titel und rechnungsbezogene Aktionen.

### Rechnung festschreiben

Einen Entwurf markieren und **Festschreiben** wählen. Im Bestätigungsdialog den
Nummernkreis und das Ausstellungsdatum kontrollieren. Erst nach der Bestätigung
wird die endgültige Rechnungsnummer vergeben. Festgeschriebene Rechnungen sind
unveränderlich und können weder bearbeitet noch erneut festgeschrieben werden.

Eine endgültige Rechnungsnummer wird nicht beim Anlegen eines Entwurfs vergeben,
sondern erst beim Festschreiben. Der Standardaufbau lautet beispielsweise
`RE-2026-000001`. Nummernkreis, Kalenderjahr und laufende Nummer werden gemeinsam
geführt. Eine festgeschriebene Rechnung kann nicht erneut festgeschrieben werden.

Nettobetrag und Umsatzsteuer werden pro Position kaufmännisch auf Cent gerundet.
Die Gesamtsummen und Steuergruppen werden aus diesen unveränderlich gespeicherten
Positionswerten gebildet.

Mit **Vorschau** zeigt Companyware die Seiten des tatsächlich erzeugten PDFs direkt
im Arbeitsbereich. Damit entsprechen Logo, A4-Abstände, Tabellenumbrüche und Footer
dem später gespeicherten Dokument. Die Vorschau funktioniert auch für Entwürfe;
**PDF speichern** bleibt weiterhin auf festgeschriebene Rechnungen beschränkt.

Eine festgeschriebene Rechnung kann mit **E-Rechnung speichern** als Hybrid-PDF
(PDF/A-3 mit eingebettetem `factur-x.xml`) und als separate XRechnung-XML
gespeichert werden. Companyware legt das Ergebnis beim ersten Aufruf
unveränderlich im lokalen Archiv ab; weitere Exporte liefern exakt dieses
Artefakt und zeigen dessen SHA-256-Prüfsumme an.

**Storno/Korrektur** verlangt einen Grund, archiviert zuerst den Originalbeleg,
markiert ihn als storniert und öffnet einen verknüpften Korrekturentwurf. Nach
dessen Festschreibung wird die strukturierte Ausgabe als Gutschrift (Typ 381) mit
Referenz auf die ursprüngliche Rechnungsnummer erzeugt. Vor dem produktiven
Versand ist die XML zusätzlich mit dem für den Empfänger geltenden offiziellen
XRechnung-/Peppol-Prüfwerkzeug zu validieren.

### Rechnungsvorlage und PDF

Die Vorlage ist eine einzelne XHTML-Datei mit eingebettetem CSS. Sie wird direkt
in Companyware als Quelltext bearbeitet; ein WYSIWYG-Editor ist bewusst nicht
vorgesehen, damit der druckbare Aufbau kontrollierbar und wiederholbar bleibt.

1. Unter **Rechnungen** den Eintrag **Vorlage bearbeiten** wählen. Links erscheint
   der Quelltext, rechts die daraus erzeugte PDF-Seite mit klar gekennzeichneten
   Beispieldaten.
2. XHTML, CSS und die angezeigten Platzhalter anpassen. Mit **PDF-Vorschau
   aktualisieren** werden auch noch nicht gespeicherte Änderungen geprüft und
   als PDF rechts dargestellt. Logo, A4-Abstände, Seitenumbrüche und Footer
   entsprechen damit dem später gespeicherten PDF. Bei ungültigem XHTML bleibt
   der Quelltext erhalten und die letzte gültige Vorschau sichtbar.
3. **Vorlage speichern** wählen. Companyware prüft Struktur, erlaubte Elemente
   und Platzhalter vor dem Speichern. Der Editor bleibt danach geöffnet und zeigt
   den Speicherstatus; **Abbrechen** führt zurück zur Übersicht.
4. In der Übersicht eine Rechnung markieren und **Vorschau** wählen.
5. Eine festgeschriebene Rechnung markieren und mit **PDF speichern** einen
   Dateinamen und Speicherort auswählen. Entwürfe können in der Vorschau geprüft,
   aber noch nicht als Rechnungs-PDF exportiert werden.

Die Vorlage darf höchstens 256 KiB groß sein und muss `{{lines}}` enthalten.
Unterstützt werden die im Editor genannten Rechnungs-, Verkäufer- und
Kundenplatzhalter. Skripte, Ereignisattribute, Links, Bilder, externe Stylesheets,
Webfonts, `data:`-Inhalte und CSS-URLs werden abgelehnt. Dadurch lädt eine Vorschau
keine Inhalte aus dem Netzwerk. **Standard wiederherstellen** ersetzt eine eigene
Vorlage nach einer Bestätigung durch die mitgelieferte Companyware-Vorlage.

Vorschau und PDF beziehen ihre Beträge ausschließlich aus dem gespeicherten
Rechnungssnapshot; die Vorlage kann weder Summen berechnen noch eine Nummer
vergeben. Wird die Vorlage später geändert, sieht ein neu erzeugtes PDF anders aus.
Mit **E-Rechnung speichern** oder dem Mailversand wird das tatsächlich ausgegebene
Dokument unveränderlich im lokalen Archiv abgelegt. Dieses Archiv ist Bestandteil
der vollständigen Datenbanksicherung.

### Firmenlogo im Briefpapier

Unter **Rechnungen → Briefpapier** wird ein Firmenlogo unabhängig vom technischen
Vorlagenquelltext gepflegt.

Bei einer neuen Installation verwendet Companyware bereits das mitgelieferte
mbdus-Logo rechts oben. Die Standardvorlage orientiert sich am bereitgestellten
Geschäftspapier: Fensteranschrift links, Kontakt- und Rechnungsdaten rechts,
graue Positions- und Summenzeilen sowie ein zweispaltiger Footer. Bereits
gespeicherte eigene Vorlagen und Briefpapierdaten werden durch ein Update nicht
überschrieben. **Standard wiederherstellen** setzt nur den Vorlagenquelltext zurück;
ein eigenes Logo bleibt erhalten.

Für eine bestehende Installation sind deshalb zwei getrennte Schritte vorgesehen:
Unter **Vorlage bearbeiten → Standard wiederherstellen** wird das mdbus-nahe
Seitenlayout aktiviert. Danach ersetzt **Briefpapier → mdbus-Standardlogo
verwenden** ausschließlich das bisherige Logo durch das mitgelieferte Original;
Firmendaten und Footer bleiben dabei erhalten.

1. **Logo auswählen** wählen und eine PNG- oder JPEG-Datei öffnen.
2. Die Breite zwischen 10 und 80 mm sowie **Links**, **Mitte** oder **Rechts**
   auswählen. Die Ansicht zeigt das ausgewählte Bild vor dem Speichern.
3. **Logo speichern** wählen. Die Rechnungsvorschau und neu erzeugte PDFs betten
   das Bild anschließend direkt ein.
4. Mit **Logo entfernen** kann das gespeicherte Bild nach Bestätigung gelöscht
   werden.

Die Datei darf höchstens 2 MiB und höchstens 6000 × 6000 Pixel groß sein. SVG,
Webadressen und Dateiverweise werden nicht akzeptiert. Das Logo liegt lokal in der
Rechnungsdatenbank; es wird weder aus dem Netzwerk geladen noch nur als Pfad
gespeichert. In einer eigenen Vorlage bestimmt `{{company.logo}}` die Position.

Meldet Companyware eine ungültige Vorlage, zuerst nicht unterstützte Elemente,
Attribute oder Platzhalter entfernen und prüfen, ob das XHTML korrekt geschlossen
ist. Bei einem Speicherfehler einen beschreibbaren Zielordner wählen; vorhandene
Dateien werden erst nach der Bestätigung im Dateidialog ersetzt.

## 7. Datensicherung

Vor Updates, Pluginmigrationen oder größeren Änderungen muss eine aktuelle
Datensicherung vorhanden sein.

- Embedded-Betrieb: Companyware vollständig beenden und anschließend das gesamte
  Datenbankverzeichnis sichern.
- Serverbetrieb: Die zentrale Sicherung muss am Derby Network Server durchgeführt
  und regelmäßig auf Wiederherstellbarkeit geprüft werden.

Eine ausführliche technische Anleitung enthält
[`derby-backup.md`](derby-backup.md).

## Abmelden und Zugang wiederherstellen

Zum Benutzerwechsel wählen Sie **Datei → Abmelden / Benutzer wechseln** oder
**Abmelden / Benutzer wechseln** unten links. Bestätigen Sie die Warnung nur,
wenn offene Eingaben gespeichert sind. Companyware leert danach Arbeitsbereich,
Status, Auswahl und sitzungsbezogene Geheimnisse. Plugins, Spring und die
Datenbank bleiben gestartet; der nächste Benutzer meldet sich im frischen Dialog an.

Erzeugen Sie im **Profil** vorsorglich einen **Recovery-Schlüssel** und bewahren
Sie ihn getrennt und geschützt auf. Der vollständige Schlüssel erscheint nur
einmal; eine Rotation macht den vorherigen sofort ungültig. Wählen Sie am Login
**Zugangsdaten vergessen?**, geben Sie Schlüssel und ein neues, richtlinienkonformes
Passwort ein. Erst nach erfolgreicher Prüfung zeigt Companyware den Benutzernamen.
Der Schlüssel ist danach verbraucht.

Für die Wiederherstellung per E-Mail muss die Adresse im **Profil** bestätigt sein.
Nach dem Speichern einer neuen oder geänderten Adresse wählen Sie **E-Mail-Adresse
bestätigen**. Companyware sendet einen einmalig nutzbaren Link. Kopieren Sie den
vollständigen Link in den angezeigten Bestätigungsdialog. Er ist 30 Minuten gültig.

Wählen Sie am Login **Zugangsdaten vergessen? → Link per E-Mail anfordern** und
geben Sie die bestätigte Adresse ein. Die Meldung ist bewusst immer gleich und
verrät nicht, ob ein Konto existiert. Öffnen Sie danach den Dialog erneut, wählen
Sie **Recovery-Schlüssel oder Link verwenden**, fügen Sie den vollständigen Link
ein und vergeben Sie das neue Passwort zweimal. Link und Recovery-Schlüssel sind
jeweils nur einmal verwendbar. Ohne bestätigte Adresse oder Schlüssel bleibt die
administrative Zurücksetzung der sichere Rückfallweg.

## 8. Häufige Probleme

### Anmeldung funktioniert nicht

- Schreibweise des Benutzernamens prüfen.
- Das Auge rechts im Passwortfeld schaltet die verdeckte und lesbare Darstellung
  des eingegebenen Passworts um.
- Feststelltaste und Tastaturlayout kontrollieren.
- Sicherstellen, dass der Benutzer aktiv ist.
- Beim Erstkennwortwechsel mindestens zehn Zeichen verwenden und beide neuen
  Eingaben identisch erfassen.

### Eingabe ist erst nach einem Klick möglich

Wenn Companyware aus Eclipse gestartet wird, kann Windows Eclipse im Vordergrund
lassen. Companyware einmal über die Taskleiste oder mit `Alt+Tab` aktivieren. Bei
einer späteren regulär installierten Anwendung soll dieser Entwicklungseffekt nicht
auftreten.

### Datenbank kann nicht geöffnet werden

- Meldet Companyware eine bereits verwendete eingebettete Datenbank, die andere
  Companyware-Instanz regulär beenden und danach erneut starten.
- Im Firmenbetrieb Erreichbarkeit des Derby Servers prüfen.
- Keine Datenbankdateien löschen. Fehlermeldung und Protokoll für die Administration
  sichern.

### Plugin wird nicht angezeigt

- Im Pluginmanager kontrollieren, ob das Plugin aktiv ist.
- Prüfen, ob eine Meldung auf ein fehlendes oder inaktives abhängiges Plugin hinweist.
- Pluginmanager nach der Installation geöffnet lassen oder erneut öffnen; die Liste
  wird nach erfolgreicher Installation sofort aktualisiert.
- Bei einem Update prüfen, ob das vorhandene Plugin deaktiviert ist und das Paket
  dieselbe ID, denselben Namen sowie eine höhere Versionsnummer verwendet.
- Bei einer Fehlermeldung das Paket nicht manuell entpacken oder verändern.

## 9. Sicherer Umgang

- Passwörter nicht weitergeben und nicht in Konfigurationsdateien notieren.
- Nur Plugins aus vertrauenswürdigen Quellen installieren.
- Vor Änderungen an Datenbank oder Plugins eine Sicherung erstellen.
- Protokolle können technische Angaben enthalten; vor öffentlicher Weitergabe
  prüfen, ob interne Servernamen oder Benutzernamen enthalten sind.
Unter **Rechnungen → Briefpapier** werden die wiederverwendbaren Firmendaten und
die Gestaltung gemeinsam gepflegt:

1. Firmenname, Anschrift, Steuer-/USt-ID und den gewünschten Footer eintragen.
2. Optional ein PNG- oder JPEG-Logo auswählen sowie Breite und Ausrichtung setzen.
3. Mit **Vorschau aktualisieren** die vollständige Beispielrechnung rechts prüfen.
   Die Vorschau verwendet auch noch nicht gespeicherte Eingaben.
4. Mit **Briefpapier speichern** alle Angaben gemeinsam übernehmen.

Neue Rechnungsentwürfe übernehmen Firmenname, Anschrift und Steuer-ID als Vorgabe.
Diese Werte können im Entwurf noch angepasst werden. Bereits gespeicherte
Rechnungen bleiben unveränderte Snapshots und werden nicht rückwirkend geändert.
Der Footer wird beim Rendern aus der aktuellen Briefpapier-Konfiguration ergänzt.
### Rechnung per E-Mail versenden

#### SMTP-Zugang einrichten

Vor dem ersten Versand **Einstellungen → E-Mail-Versand** öffnen. SMTP-Server,
Port, Absendername, Absenderadresse und – falls der Mailserver eine Anmeldung verlangt –
Benutzername und Passwort eintragen. **STARTTLS verwenden** sollte für normale
Produktivserver eingeschaltet bleiben.

Beispielsweise erzeugt der Name `Mathias Bauer - mbdus` zusammen mit der Adresse
`mathias.bauer@mbdus.de` den sichtbaren Absender
`Mathias Bauer - mbdus <mathias.bauer@mbdus.de>`.

Mit **Verbindung testen** werden die aktuellen Formularwerte geprüft, ohne sie zu
speichern oder eine Rechnung zu versenden. **Einstellungen speichern** übernimmt
Server, Port, Absender, Benutzername und TLS-Auswahl in die Companyware-Datenbank.
Das Passwort wird aus Sicherheitsgründen nicht in der Datenbank gespeichert. Es
bleibt nur bis zum Beenden von Companyware im Arbeitsspeicher; nach einem Neustart
muss es erneut eingegeben werden. Alternativ kann die Administration weiterhin
`CW_SECRET_INVOICE_SMTP_PASSWORD` bereitstellen.

Bei einem Verbindungsfehler Servername, Port, STARTTLS, Benutzername und Passwort
prüfen. Zusätzlich muss der Mailserver die eingetragene Absenderadresse für dieses
Konto erlauben. Ein erfolgreicher Verbindungstest bestätigt die Anmeldung, nicht
die Zustellung an einen konkreten Empfänger.

Unter **Rechnungen → Mailvorlage** werden Betreff und Nachricht als Klartext
bearbeitet. Verfügbar sind `{{invoice.number}}`, `{{invoice.gross}}`,
`{{invoice.currency}}`, `{{customer.name}}` und `{{seller.name}}`. Unbekannte
Platzhalter werden beim Speichern abgewiesen.

In der Übersicht eine festgeschriebene Rechnung markieren und **Per E-Mail
senden** wählen. Der modale Prüfdialog zeigt Empfänger, gerenderten Betreff und
Nachricht vollständig editierbar. Das archivierte PDF/A-3 wird immer angehängt;
die separate XRechnung-XML lässt sich abwählen. Erst **OK** startet den Versand.
Ein erfolgreicher Versand wird ohne Nachrichtentext, Anhang oder Zugangsdaten im
lokalen Versandprotokoll festgehalten. Entwürfe können nicht versendet werden.
