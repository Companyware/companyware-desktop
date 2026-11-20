package plugins.community.invoice;

import platform.api.service.ServiceKey;
import plugins.community.invoice.application.InvoiceDocumentService;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.application.EInvoiceService;
import plugins.community.invoice.application.InvoiceMailService;

/** Typed service identity for the invoice application boundary. */
public final class InvoiceServiceIds {
    public static final ServiceKey<InvoiceService> INVOICES =
            ServiceKey.of("companyware.invoice.service", InvoiceService.class);
    public static final ServiceKey<InvoiceDocumentService> DOCUMENTS =
            ServiceKey.of("companyware.invoice.documents", InvoiceDocumentService.class);
    public static final ServiceKey<EInvoiceService> E_INVOICES =
            ServiceKey.of("companyware.invoice.e-invoices", EInvoiceService.class);
    public static final ServiceKey<InvoiceMailService> MAIL =
            ServiceKey.of("companyware.invoice.mail", InvoiceMailService.class);
    private InvoiceServiceIds() { }
}
