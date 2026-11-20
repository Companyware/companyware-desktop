package plugins.community.invoice.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete immutable snapshot used by the draft editor and later renderers. */
public final class InvoiceDetailsDto {
    private final InvoiceDto summary;
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerTaxId;
    private final String customerAddress;
    private final LocalDate serviceDate;
    private final String currency;
    private final String paymentTerms;
    private final List<InvoiceLineDto> lines;

    public InvoiceDetailsDto(InvoiceDto summary, String sellerName, String sellerAddress,
            String sellerTaxId, String customerAddress, LocalDate serviceDate,
            String currency, String paymentTerms, List<InvoiceLineDto> lines) {
        this.summary = summary;
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerTaxId = sellerTaxId;
        this.customerAddress = customerAddress;
        this.serviceDate = serviceDate;
        this.currency = currency;
        this.paymentTerms = paymentTerms;
        this.lines = Collections.unmodifiableList(new ArrayList<InvoiceLineDto>(lines));
    }
    public InvoiceDto summary() { return summary; }
    public String sellerName() { return sellerName; }
    public String sellerAddress() { return sellerAddress; }
    public String sellerTaxId() { return sellerTaxId; }
    public String customerAddress() { return customerAddress; }
    public LocalDate serviceDate() { return serviceDate; }
    public String currency() { return currency; }
    public String paymentTerms() { return paymentTerms; }
    public List<InvoiceLineDto> lines() { return lines; }
}

