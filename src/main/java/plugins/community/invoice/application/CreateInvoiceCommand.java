package plugins.community.invoice.application;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete business snapshot required to create an editable invoice draft. */
public final class CreateInvoiceCommand {
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerTaxId;
    private final String customerName;
    private final String customerAddress;
    private final LocalDate serviceDate;
    private final String currency;
    private final String paymentTerms;
    private final List<InvoiceLineInput> lines;

    public CreateInvoiceCommand(String sellerName, String sellerAddress, String sellerTaxId,
            String customerName, String customerAddress, LocalDate serviceDate,
            String currency, String paymentTerms, List<InvoiceLineInput> lines) {
        this.sellerName = sellerName;
        this.sellerAddress = sellerAddress;
        this.sellerTaxId = sellerTaxId;
        this.customerName = customerName;
        this.customerAddress = customerAddress;
        this.serviceDate = serviceDate;
        this.currency = currency;
        this.paymentTerms = paymentTerms;
        this.lines = lines == null ? Collections.<InvoiceLineInput>emptyList()
                : Collections.unmodifiableList(new ArrayList<InvoiceLineInput>(lines));
    }
    public String sellerName() { return sellerName; }
    public String sellerAddress() { return sellerAddress; }
    public String sellerTaxId() { return sellerTaxId; }
    public String customerName() { return customerName; }
    public String customerAddress() { return customerAddress; }
    public LocalDate serviceDate() { return serviceDate; }
    public String currency() { return currency; }
    public String paymentTerms() { return paymentTerms; }
    public List<InvoiceLineInput> lines() { return lines; }
}
