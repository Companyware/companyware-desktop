package plugins.community.invoice.application;

import platform.api.application.ApplicationResult;
import platform.api.service.Service;

/** Structured invoice, immutable archive and correction use cases. */
public interface EInvoiceService extends Service {
    ApplicationResult<EInvoiceArchiveDto> archive(long invoiceId);
    ApplicationResult<EInvoiceArchiveDto> archived(long invoiceId);
    ApplicationResult<InvoiceDetailsDto> createCorrectionDraft(long originalInvoiceId, String reason);
}
