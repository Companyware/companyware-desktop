package plugins.community.invoice.application;

import java.time.LocalDate;
import java.util.List;
import platform.api.application.ApplicationResult;
import platform.api.service.Service;

/** Transport-neutral invoice use cases; Swing is added as a separate adapter. */
public interface InvoiceService extends Service {
    ApplicationResult<InvoiceDto> createDraft(CreateInvoiceCommand command);
    ApplicationResult<InvoiceDto> updateDraft(long id, CreateInvoiceCommand command);
    ApplicationResult<List<InvoiceDto>> list();
    ApplicationResult<InvoiceDto> find(long id);
    ApplicationResult<InvoiceDetailsDto> details(long id);
    ApplicationResult<InvoiceDto> finalizeDraft(long id, String numberSeries, LocalDate issueDate);
}
