package plugins.community.invoice.application;

import platform.api.application.ApplicationResult;
import platform.api.service.Service;

/** Editable invoice-mail template, review draft and explicit sending boundary. */
public interface InvoiceMailService extends Service {
    ApplicationResult<InvoiceMailTemplateDto> template();
    ApplicationResult<InvoiceMailTemplateDto> saveTemplate(InvoiceMailTemplateDto template);
    ApplicationResult<InvoiceMailTemplateDto> restoreDefaultTemplate();
    ApplicationResult<InvoiceMailDraftDto> draft(long invoiceId);
    ApplicationResult<Void> send(InvoiceMailDraftDto draft, boolean attachStructuredXml);
    ApplicationResult<SmtpSettingsDto> smtpSettings();
    ApplicationResult<SmtpSettingsDto> saveSmtpSettings(SmtpSettingsDto settings, char[] password);
    ApplicationResult<Void> testSmtpSettings(SmtpSettingsDto settings, char[] password);
}
