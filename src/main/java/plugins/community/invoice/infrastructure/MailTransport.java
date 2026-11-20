package plugins.community.invoice.infrastructure;

import plugins.community.invoice.application.EInvoiceArchiveDto;
import plugins.community.invoice.application.InvoiceMailDraftDto;
import plugins.community.invoice.application.SmtpSettingsDto;

/** Infrastructure seam for SMTP and deterministic tests. */
interface MailTransport {
    void send(InvoiceMailDraftDto draft, EInvoiceArchiveDto artifact, boolean attachStructuredXml) throws Exception;
    default void test(SmtpSettingsDto settings, char[] password) throws Exception { }
}
