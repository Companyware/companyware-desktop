# So dokumentieren wir Companyware

Companyware orientiert seine Dokumentation am leicht zugänglichen, thematisch
gegliederten Ansatz etablierter Produktdokumentationen wie Shopware. Texte werden
selbst formuliert und an Companyware angepasst.

## Grundregel

Jede neue oder geänderte Funktion wird im selben Arbeitspaket dokumentiert:

- Bedienung und sichtbares Verhalten im Benutzerhandbuch;
- öffentliche Verträge, Konfiguration, Erweiterungspunkte und Tests im
  Entwicklerhandbuch;
- Installation, Betrieb, Sicherung und Fehlerbehebung in der Betriebsdokumentation;
- dauerhafte Architekturentscheidungen zusätzlich im Knowledge-Repository.

Reine interne Änderungen ohne Auswirkung auf Bedienung oder öffentliche Verträge
benötigen mindestens einen nachvollziehbaren Eintrag in Entwicklerdokumentation
oder Änderungshistorie.

## Aufbau eines Hilfethemas

Ein Thema beantwortet in dieser Reihenfolge:

1. **Worum geht es?** Ein kurzer Satz ohne Fachjargon.
2. **Was wird benötigt?** Rechte, Betriebsart oder andere Voraussetzungen.
3. **Wie geht es?** Nummerierte, ausführbare Schritte mit den sichtbaren Namen der
   Schaltflächen und Menüs.
4. **Was ist das Ergebnis?** Woran erkennt der Benutzer den Erfolg?
5. **Was kann schiefgehen?** Konkrete Meldungen, Ursachen und sichere Abhilfe.
6. **Was gehört dazu?** Links zu passenden Grundlagen und weiterführenden Themen.

## Schreibweise

- Kurze Sätze und aktive Formulierungen verwenden.
- Eine Aufgabe pro Abschnitt erklären.
- Zuerst die normale Bedienung, danach technische Hintergründe beschreiben.
- Menüeinträge und Schaltflächen fett kennzeichnen.
- Befehle, Dateinamen, Einstellungen und IDs als Code formatieren.
- Keine unbestimmten Aussagen wie „einfach konfigurieren“ ohne die nötigen Schritte.
- Sicherheitswarnungen direkt vor die riskante Handlung setzen.
- Keine Passwörter, privaten Schlüssel oder echten internen Serverdaten abdrucken.

## Fertig-Kriterium eines Arbeitspakets

Vor Abschluss wird geprüft:

- Ist jede neue Benutzeraktion erklärt?
- Sind Voraussetzungen und Berechtigungen genannt?
- Sind Konfigurationsschlüssel und Standardwerte dokumentiert?
- Gibt es eine verständliche Fehlerhilfe für erwartbare Probleme?
- Sind Beispiele aktuell und mit dem implementierten Stand geprüft?
- Verweisen README und Dokumentationsübersicht auf neue Hauptthemen?
- Wurden Entwickler- und Knowledge-Dokumentation gemeinsam mit dem Code versioniert?

Nicht zutreffende Punkte werden bewusst als nicht zutreffend bewertet; sie werden
nicht stillschweigend ausgelassen.
