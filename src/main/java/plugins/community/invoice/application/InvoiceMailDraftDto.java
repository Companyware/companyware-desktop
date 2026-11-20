package plugins.community.invoice.application;

/** Rendered, reviewable mail draft for one archived invoice. */
public final class InvoiceMailDraftDto {
    private final long invoiceId;
    private final String recipient;
    private final String subject;
    private final String body;
    public InvoiceMailDraftDto(long invoiceId, String recipient, String subject, String body) {
        this.invoiceId = invoiceId; this.recipient = recipient; this.subject = subject; this.body = body;
    }
    public long invoiceId() { return invoiceId; }
    public String recipient() { return recipient; }
    public String subject() { return subject; }
    public String body() { return body; }
}
