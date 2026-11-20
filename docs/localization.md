# Übersetzungen in Companyware

## Für Plugin-Entwickler

Jeder sichtbare Text erhält einen stabilen Schlüssel in einer plugin-eigenen
`messages_*.properties`-Datei. Englisch ist der technische Fallback. Deutsch und
Englisch besitzen deshalb immer dieselbe Schlüsselmenge und werden automatisch
geprüft. Übersetzte Texte dürfen nicht als Action-Command, Berechtigung oder
Datenbankschlüssel verwendet werden.

Beispiel:

```java
String title = context.messages().text("orders.overview.title");
```

Lang lebende Ansichten registrieren einen Änderungs-Listener beim öffentlichen
`LocalizationService` und geben ihn über `closeOnStop` wieder frei. Neu aufgebaute
Ansichten lesen die Texte einfach erneut.

## Sprachstufen

Produktiv auswählbar sind zunächst:

1. Deutsch (`de`) und Englisch (`en`) – verbindliche, testgeprüfte Basis.
2. Französisch (`fr`) und Spanisch (`es`) – erste zusätzliche Sprachpakete.

Der geplante Katalog umfasst 28 Sprachvarianten:

`de`, `en`, `fr`, `es`, `it`, `nl`, `pt`, `pt-BR`, `pl`, `cs`, `sk`, `hu`,
`ro`, `bg`, `hr`, `sl`, `da`, `sv`, `nb`, `fi`, `et`, `lv`, `lt`, `el`,
`tr`, `ru`, `uk`, `zh-CN`.

Russisch (`ru`) und vereinfachtes Chinesisch (`zh-CN`) sind damit ausdrücklich
Teil der Zielausstattung. Eine Sprache wird erst in die Auswahl aufgenommen, wenn
ihre Kerntexte und die aktiven Plugin-Bundles fachlich und sprachlich geprüft sind.
Bis dahin verhindert der englische Fallback leere oder technische Anzeigen.

## Betrieb und Datenbank

Die Auswahl wird als BCP-47-Sprachtag, beispielsweise `de` oder `pt-BR`, am
Benutzer gespeichert. Migration 5 des Identity-Schemas ergänzt dafür die Spalte
`Users.locale`; bestehende Benutzer erhalten `de`. Dieselbe Migration läuft im
Embedded- und Client/Server-Profil. Ein Backup vor Schema-Upgrades bleibt Pflicht.
