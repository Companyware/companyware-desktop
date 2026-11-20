package plugins.community.invoice.application;

import platform.api.application.ApplicationResult;
import platform.api.service.Service;

/** Transport-neutral template, preview and PDF use cases. */
public interface InvoiceDocumentService extends Service {
    ApplicationResult<InvoiceTemplateDto> template();
    ApplicationResult<InvoiceTemplateDto> saveTemplate(String source);
    ApplicationResult<InvoiceTemplateDto> restoreDefaultTemplate();
    ApplicationResult<InvoiceBrandingDto> branding();
    ApplicationResult<InvoiceBrandingDto> saveLogo(byte[] content, String mediaType,
            int widthMillimeters, String alignment);
    ApplicationResult<InvoiceBrandingDto> saveBranding(InvoiceBrandingDto branding);
    ApplicationResult<InvoiceBrandingDto> restoreDefaultLogo();
    ApplicationResult<InvoiceBrandingDto> removeLogo();
    ApplicationResult<String> previewBrandingHtml(InvoiceBrandingDto branding);
    ApplicationResult<String> previewTemplateHtml(String source);
    ApplicationResult<InvoicePdfDto> previewTemplatePdf(String source);
    ApplicationResult<String> previewHtml(long invoiceId);
    ApplicationResult<InvoicePdfDto> previewPdf(long invoiceId);
    ApplicationResult<InvoicePdfDto> createPdf(long invoiceId);
}
