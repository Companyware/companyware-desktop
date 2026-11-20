package plugins.community.invoice.application;

/** Persisted non-secret SMTP endpoint and account settings. */
public final class SmtpSettingsDto {
    private final String host;
    private final int port;
    private final String fromName;
    private final String from;
    private final String username;
    private final boolean startTls;
    private final boolean passwordAvailable;

    public SmtpSettingsDto(String host, int port, String from, String username,
            boolean startTls, boolean passwordAvailable) {
        this(host, port, "", from, username, startTls, passwordAvailable);
    }
    public SmtpSettingsDto(String host, int port, String fromName, String from, String username,
            boolean startTls, boolean passwordAvailable) {
        this.host = host; this.port = port; this.fromName = fromName; this.from = from; this.username = username;
        this.startTls = startTls; this.passwordAvailable = passwordAvailable;
    }
    public String host() { return host; }
    public int port() { return port; }
    public String fromName() { return fromName; }
    public String from() { return from; }
    public String username() { return username; }
    public boolean startTls() { return startTls; }
    public boolean passwordAvailable() { return passwordAvailable; }
}
