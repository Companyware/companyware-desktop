package plugins.community.invoice.application;

import java.time.LocalDateTime;

/** Editable controlled XHTML/CSS invoice template. */
public final class InvoiceTemplateDto {
    private final String source;
    private final boolean customized;
    private final LocalDateTime updatedAt;

    public InvoiceTemplateDto(String source, boolean customized, LocalDateTime updatedAt) {
        this.source = source;
        this.customized = customized;
        this.updatedAt = updatedAt;
    }

    public String source() { return source; }
    public boolean customized() { return customized; }
    public LocalDateTime updatedAt() { return updatedAt; }
}
