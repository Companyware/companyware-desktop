# Öffentliches Desktop-Framework veröffentlichen

Companyware ist die Produktfamilie. Dieses Dokument gilt ausschließlich für
**Companyware Desktop**. **Companyware Web** wird unabhängig veröffentlicht; ein
Desktop-Commit, -Tag oder -Artefakt ist niemals Web-Release-Evidenz. Eine spätere
Zusammenarbeit verwendet gesondert versionierte API-Verträge statt gemeinsamer
Pluginbinärdateien oder Datenbanktabellen.

## Veröffentlichter Umfang

Der stabile Erweiterungseinstieg für Desktop-Plugins ist ausschließlich das
Java-Paket `platform.api.*` zusammen mit dem Descriptorformat aus
[`plugin-sdk-v1.md`](plugin-sdk-v1.md). Klassen unter `core.*`, `desktop.*`,
`pluginmanager.*`, `models.*` und `plugins.*` sind Implementierungsdetails. Ein
Plugin darf sie weder importieren noch durch Reflexion als Vertrag behandeln.

SDK v1 garantiert innerhalb der Hauptversion die binäre und fachliche
Kompatibilität bereits veröffentlichter öffentlicher Typen. Neue optionale
Verträge dürfen hinzukommen. Inkompatible Änderungen, das Entfernen eines
Vertrags oder eine neue verpflichtende Methode erfordern eine neue SDK-Hauptversion
mit Migrationsanleitung. Veraltete Verträge werden vor einer Entfernung zunächst
als deprecated markiert und mindestens in einer veröffentlichten Minor-Version
weiter unterstützt.

## Plugin- und Paketgrenze

Ein veröffentlichbares Plugin besitzt eine dauerhafte Plugin-ID, eine semantische
Version und eine passende `requiredApiVersion`. Es wird als `.cwp` mit genau einem
Descriptor `plugin.yaml` und einer Implementierung `plugin.jar` gebaut. Aktuell
gilt ausschließlich das ausdrücklich bestätigte Vertrauensmodell `trusted-local`.
Eine öffentliche Drittanbieterdistribution ist erst zulässig, wenn Signaturen,
Publisher-Identität, Widerruf und Updatehinweise das Release-Gate erfüllen.

Plugins verwenden UI-, Persistenz-, Security-, Lokalisierungs- und
Lifecycle-Funktionen über `platform.api.*`. Fachliche Anwendungsports bleiben frei
von Swing, JDBC, Derby und HTTP. Dadurch kann ein späterer Remote-Adapter ergänzt
werden, ohne View oder Use Case neu zu schreiben.

## Lizenzregel für neue Dateien

Neue geeignete Java-, PowerShell-, Batch-, JavaScript-, TypeScript- und CSS-Dateien
beginnen vor dem Code mit dem sechszeiligen Companyware-Kommentar. Er nennt
Companyware, Copyright, `SPDX-License-Identifier: MIT` und die Root-Datei
`LICENSE`. Formate ohne Kommentare verwenden ihre vorgesehene Lizenzmetadatenform.
Fremd- und generierter Code behält seine ursprüngliche Provenienz und wird nicht
mechanisch umgelabelt.

`tools/verify-source-policy.ps1` prüft neu hinzugefügte geeignete Dateien gegenüber
einer Basisreferenz. In CI wird diese explizit gesetzt:

```powershell
$env:CW_POLICY_BASE_REF = 'origin/develop'
.\tools\verify-source-policy.ps1
```

## Release-Gate

Ein Frameworkstand ist erst veröffentlichungsbereit, wenn:

1. der Arbeitsbaum und der geprüfte Commit eindeutig sind;
2. `tools/verify-framework-release.ps1` mit einem JDK erfolgreich läuft;
3. alle Maven-, Architektur-, Lifecycle-, Paket- und Derby-Tests bestehen;
4. der Clean-Clone-Nachweis aus `release-process.md` besteht;
5. Benutzer-, Entwickler-, Betriebs-, Upgrade- und Recovery-Dokumentation aktuell sind;
6. Lizenz-, Security- und Geheimnisprüfung keine offenen Befunde enthält;
7. Release Notes, SemVer-Tag und SHA-256-Prüfsummen vorbereitet sind;
8. GitHub und Website exakt denselben geprüften Commit ausweisen.

Zusätzlich verweigert das vollständige Gate Maven-Versionen mit `-SNAPSHOT`.
Entwicklungs-CI darf `-AllowSnapshot` verwenden, erzeugt damit aber ausdrücklich
nur kurzlebige Kandidatenartefakte. Vor Tag und Veröffentlichung müssen
`docs/release-notes/UNRELEASED.md`, Versionsnummer und Prüfsummen in endgültige,
commitgebundene Release-Evidenz überführt werden.

Auf Rechnern ohne JDK kann nur die nicht hinreichende Vorprüfung ausgeführt werden:

```powershell
.\tools\verify-framework-release.ps1 -PolicyOnly
```

Sie bestätigt Richtlinie und Dokumentationsbestand, ersetzt aber niemals Build,
Tests, Clean Clone oder Laufzeitprüfung.
