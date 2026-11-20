package plugins.community.invoice.application;

import java.time.LocalDateTime;

/** Immutable archived hybrid e-invoice and standalone XRechnung payload. */
public final class EInvoiceArchiveDto {
    private final long invoiceId;
    private final String filename;
    private final byte[] pdf;
    private final byte[] xml;
    private final String sha256;
    private final LocalDateTime archivedAt;

    public EInvoiceArchiveDto(long invoiceId, String filename, byte[] pdf, byte[] xml,
            String sha256, LocalDateTime archivedAt) {
        this.invoiceId = invoiceId; this.filename = filename;
        this.pdf = pdf.clone(); this.xml = xml.clone(); this.sha256 = sha256; this.archivedAt = archivedAt;
    }
    public long invoiceId() { return invoiceId; }
    public String filename() { return filename; }
    public byte[] pdf() { return pdf.clone(); }
    public byte[] xml() { return xml.clone(); }
    public String sha256() { return sha256; }
    public LocalDateTime archivedAt() { return archivedAt; }
}
