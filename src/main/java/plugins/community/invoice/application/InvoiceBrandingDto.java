package plugins.community.invoice.application;

/** Immutable, locally stored invoice letterhead logo settings. */
public final class InvoiceBrandingDto {
    private final byte[] logo;
    private final String mediaType;
    private final int widthMillimeters;
    private final String alignment;
    private final String sellerName;
    private final String sellerAddress;
    private final String sellerTaxId;
    private final String footer;

    public InvoiceBrandingDto(byte[] logo, String mediaType, int widthMillimeters, String alignment) {
        this(logo, mediaType, widthMillimeters, alignment, "", "", "", "");
    }

    public InvoiceBrandingDto(byte[] logo, String mediaType, int widthMillimeters, String alignment,
            String sellerName, String sellerAddress, String sellerTaxId, String footer) {
        this.logo = logo == null ? null : logo.clone();
        this.mediaType = mediaType;
        this.widthMillimeters = widthMillimeters;
        this.alignment = alignment;
        this.sellerName = sellerName == null ? "" : sellerName;
        this.sellerAddress = sellerAddress == null ? "" : sellerAddress;
        this.sellerTaxId = sellerTaxId == null ? "" : sellerTaxId;
        this.footer = footer == null ? "" : footer;
    }
    public byte[] logo() { return logo == null ? null : logo.clone(); }
    public boolean hasLogo() { return logo != null && logo.length > 0; }
    public String mediaType() { return mediaType; }
    public int widthMillimeters() { return widthMillimeters; }
    public String alignment() { return alignment; }
    public String sellerName() { return sellerName; }
    public String sellerAddress() { return sellerAddress; }
    public String sellerTaxId() { return sellerTaxId; }
    public String footer() { return footer; }
}
