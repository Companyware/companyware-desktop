# Security policy

## Supported versions

Companyware Desktop has not published a supported stable release yet. The current
repository state is a development preview. Security fixes are prepared on the
authoritative development branch and become supported only when an immutable
release is explicitly listed here.

| Version | Supported |
| --- | --- |
| Development preview | No production support |

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability, leaked credential or
private customer data. Use GitHub's **Report a vulnerability** function in the
Security tab of this repository. Include the affected commit or version,
reproduction steps, impact and any known workaround. Do not include real passwords,
tokens, databases, invoices or customer documents.

If private reporting is unavailable, contact the repository owner through the
address published in the GitHub organization profile and request a private
security channel before sending details.

We will acknowledge receipt, classify affected versions and coordinate disclosure.
No response-time or paid-support guarantee applies before a supported release is
published.

## Credentials and secrets

Never publish or reuse bootstrap credentials. Existing preview installations may
contain a legacy bootstrap account and must replace its initial password before a
session is established. New users receive temporary credentials from their own
administrator and must replace them on first login.

SMTP passwords, database credentials, recovery keys, API tokens and signing keys
must not be committed, pasted into issues or included in logs. Follow the secret
provider and recovery guidance in `docs/developer-guide.md` and
`docs/benutzerhandbuch.md`.

## Plugin trust boundary

Installed Desktop plugins are trusted in-process Java code, not sandboxed content.
The current `trusted-local` package policy requires an explicit operator decision.
Public third-party distribution remains blocked until publisher identity,
signatures, revocation and update/advisory handling pass their release gates.
