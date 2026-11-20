package plugins.community.invoice.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Immutable invoice summary crossing the application boundary. */
public final class InvoiceDto {
    private final long id;
    private final String status;
    private final String invoiceNumber;
    private final String customerName;
    private final LocalDate issueDate;
    private final BigDecimal net;
    private final BigDecimal tax;
    private final BigDecimal gross;

    public InvoiceDto(long id, String status, String invoiceNumber, String customerName, LocalDate issueDate,
            BigDecimal net, BigDecimal tax, BigDecimal gross) {
        this.id = id;
        this.status = status;
        this.invoiceNumber = invoiceNumber;
        this.customerName = customerName;
        this.issueDate = issueDate;
        this.net = net;
        this.tax = tax;
        this.gross = gross;
    }
    public long id() { return id; }
    public String status() { return status; }
    public String invoiceNumber() { return invoiceNumber; }
    public String customerName() { return customerName; }
    public LocalDate issueDate() { return issueDate; }
    public BigDecimal net() { return net; }
    public BigDecimal tax() { return tax; }
    public BigDecimal gross() { return gross; }
}
