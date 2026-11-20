package plugins.community.invoice.application;

/** Immutable generated PDF payload and safe suggested filename. */
public final class InvoicePdfDto {
    private final String filename;
    private final byte[] content;

    public InvoicePdfDto(String filename, byte[] content) {
        this.filename = filename;
        this.content = content.clone();
    }

    public String filename() { return filename; }
    public byte[] content() { return content.clone(); }
}
