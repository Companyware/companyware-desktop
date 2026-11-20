package plugins.community.companywaredemo.application;

/** Immutable transport-neutral projection used by the overview use case. */
public final class DemoRow {
    private final String[] values;
    public DemoRow(String... values) { this.values = values.clone(); }
    public String value(int index) { return values[index]; }
    public int size() { return values.length; }
    public Object[] toTableRow() { return values.clone(); }
}
