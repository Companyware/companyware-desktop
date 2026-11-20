# Derby database backup and restore

Companyware supports `embedded` and `client-server` Derby profiles. Backup and restore always target the database selected by the active profile.

For embedded/private use, close Companyware before making a filesystem copy of the complete configured database directory (by default `data/companyware`). Only one process may open that database at a time; do not place it on a shared/network filesystem.

The desktop acquires a separate Companyware instance lock before Derby starts. If
the same embedded database is already owned, the second process stops with a clear
message. Close the first process normally; never delete `db.lck` or an application
lock file to force access. The client-server profile does not use this local guard.

For client/server/company use, backup paths are resolved on the machine running the independently operated Derby Network Server, not on a desktop client. Multiple clients may connect concurrently; Derby Network Server lifecycle, access control and backups are server-administration responsibilities.

## Online backup

1. Create a new, timestamped destination on the Derby server, for example `C:/companyware-backups/2026-08-17T103500`.
2. Connect to the `companyware` database as its owner using Derby `ij` or another SQL client.
3. Execute:

   ```sql
   CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(
       'C:/companyware-backups/2026-08-17T103500'
   );
   ```

4. Wait for the call to complete successfully. The destination must not contain an older backup with the same database name.
5. Retain the previous known-good backup until the new copy has been restored and checked in a separate environment.
6. Record the Source commit, database schema/application version and backup path together.

Derby's backup procedure creates a transactionally consistent online copy. Always use an absolute server-side path so the destination is unambiguous.

## Offline backup alternative

1. Close every Companyware client.
2. Shut down the Derby database and Network Server cleanly.
3. Copy the complete `companyware` database directory, including its `log` and `seg0` directories, to a timestamped backup location.
4. Restart the server only after the copy has completed.

Never copy a live Derby database directory with ordinary file-copy tools.

## Restore rehearsal

1. Stop all clients and the Derby Network Server.
2. Preserve the current database directory instead of overwriting the only copy.
3. Copy the backed-up `companyware` database directory into a separate Derby system home.
4. Start a temporary Network Server against that system home.
5. Connect with a non-production Companyware instance and verify login, users, settings and both community plugins.
6. Only after that rehearsal, replace the production database directory while the production server remains stopped.

The backup does not replace source control and must never be committed to this repository.

## Zugangsdaten und Recovery-Schlüssel

Recovery-Schlüssel gehören nicht in die Datenbanksicherung, in Tickets oder
Protokolle. Benutzer speichern den einmal angezeigten Vollwert in einem geeigneten
Passwortmanager. Die Datenbank enthält nur Selektor, BCrypt-Hash, Status,
Fehlversuche, Sperr- und Zeitangaben. Nach fünf Fehlversuchen sperrt Companyware
den Schlüssel für 15 Minuten; erfolgreiche Verwendung verbraucht ihn atomar.

Ohne gültigen Schlüssel verwendet die Administration den bestehenden
administrativen Passwort-Reset. Der Benutzer muss das temporäre Passwort beim
nächsten Login ändern. Betreiber dürfen weder E-Mail-Adressen als Nachweis
verwenden noch aus Fehlermeldungen Konten bestätigen. Vor einer Datenbank-Recovery
ist der normale Sicherungs- und Restore-Prozess oben zu verwenden; manuelle
SQL-Änderungen an Passwort- oder Recovery-Tabellen sind kein unterstützter Weg.

### SMTP für E-Mail-Bestätigung und Kontowiederherstellung

Die Administration stellt die folgenden Werte als Umgebungsvariablen oder als
gleichnamige JVM-Eigenschaften unter `companyware.recovery.smtp.*` bereit:

- `CW_RECOVERY_SMTP_HOST` (erforderlich)
- `CW_RECOVERY_SMTP_PORT` (Standard `587`)
- `CW_RECOVERY_SMTP_FROM` (erforderliche, autorisierte Absenderadresse)
- `CW_RECOVERY_SMTP_USERNAME` (optional)
- `CW_RECOVERY_SMTP_STARTTLS` (Standard `true`)
- `CW_SECRET_RECOVERY_SMTP_PASSWORD` (erforderlich bei SMTP-Anmeldung)
- `CW_RECOVERY_LINK_BASE` (Standard `companyware://account-recovery`)

Der SMTP-Zugang für Rechnungen wird bewusst nicht automatisch übernommen: Core
darf nicht von einem optionalen Rechnungsplugin abhängen. Der Mailserver muss TLS,
Absenderautorisierung und die Zustellung an die hinterlegten Adressen unterstützen.
Nach der Konfiguration mit einem Testkonto E-Mail-Bestätigung, Ablauf,
Einmalverwendung und Wiederherstellung prüfen. Protokolle dürfen weder Token noch
Passwörter enthalten. Bei fehlender Mailkonfiguration funktionieren Offline-Key
und administrative Zurücksetzung weiter.
