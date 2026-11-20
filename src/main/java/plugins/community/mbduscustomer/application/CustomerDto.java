package plugins.community.mbduscustomer.application;

/** Immutable customer data crossing the application boundary. */
public final class CustomerDto {
    private final Long id;
    private final String[] fields;

    public CustomerDto(Long id, String... fields) {
        if (fields.length != 9) throw new IllegalArgumentException("A customer has exactly nine fields");
        this.id = id;
        this.fields = fields.clone();
    }

    public Long id() { return id; }
    public String field(int index) { return fields[index]; }
    public Object[] toTableRow() {
        Object[] row = new Object[10];
        row[0] = id;
        System.arraycopy(fields, 0, row, 1, fields.length);
        return row;
    }
}
