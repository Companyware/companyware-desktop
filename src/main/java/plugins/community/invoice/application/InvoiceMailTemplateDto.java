package plugins.community.invoice.application;

/** Editable mail subject and plain-text body template. */
public final class InvoiceMailTemplateDto {
    private final String subject;
    private final String body;
    public InvoiceMailTemplateDto(String subject, String body) { this.subject = subject; this.body = body; }
    public String subject() { return subject; }
    public String body() { return body; }
}
