package plugins.community.invoice.infrastructure;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.invoice.application.CreateInvoiceCommand;
import plugins.community.invoice.application.InvoiceDetailsDto;
import plugins.community.invoice.application.InvoiceDto;
import plugins.community.invoice.application.InvoiceLineDto;
import plugins.community.invoice.application.InvoiceLineInput;
import plugins.community.invoice.application.InvoiceService;
import plugins.community.invoice.domain.CalculatedInvoiceLine;
import plugins.community.invoice.domain.InvoiceCalculator;
import plugins.community.invoice.domain.InvoiceTotals;

/** Local transactional JDBC adapter for invoice drafts and final number allocation. */
public final class JdbcInvoiceService implements InvoiceService {
    private final PluginDatabase database;
    private final InvoiceCalculator calculator;

    public JdbcInvoiceService(PluginDatabase database) { this(database, new InvoiceCalculator()); }
    JdbcInvoiceService(PluginDatabase database, InvoiceCalculator calculator) {
        this.database = database;
        this.calculator = calculator;
    }

    @Override public ApplicationResult<InvoiceDto> createDraft(CreateInvoiceCommand command) {
        try {
            validate(command);
            final InvoiceTotals totals = calculate(command.lines());
            long id = database.write(connection -> {
                String sql = "INSERT INTO CW_INVOICE (STATUS,SERVICE_DATE,CURRENCY,SELLER_NAME,SELLER_ADDRESS,SELLER_TAX_ID,CUSTOMER_NAME,CUSTOMER_ADDRESS,PAYMENT_TERMS,NET_TOTAL,TAX_TOTAL,GROSS_TOTAL,CREATED_AT) VALUES ('DRAFT',?,?,?,?,?,?,?,?,?,?,?,?)";
                long invoiceId;
                try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setDate(1, Date.valueOf(command.serviceDate()));
                    statement.setString(2, command.currency().toUpperCase(Locale.ROOT));
                    statement.setString(3, command.sellerName().trim());
                    statement.setString(4, command.sellerAddress().trim());
                    statement.setString(5, command.sellerTaxId().trim());
                    statement.setString(6, command.customerName().trim());
                    statement.setString(7, command.customerAddress().trim());
                    statement.setString(8, optional(command.paymentTerms()));
                    statement.setBigDecimal(9, totals.net());
                    statement.setBigDecimal(10, totals.tax());
                    statement.setBigDecimal(11, totals.gross());
                    statement.setTimestamp(12, new Timestamp(System.currentTimeMillis()));
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new IllegalStateException("Invoice id was not generated");
                        invoiceId = keys.getLong(1);
                    }
                }
                insertLines(connection, invoiceId, totals);
                return invoiceId;
            });
            return find(id);
        } catch (IllegalArgumentException validation) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, validation.getMessage());
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice draft could not be created");
        }
    }

    @Override public ApplicationResult<InvoiceDto> updateDraft(long id, CreateInvoiceCommand command) {
        try {
            validate(command);
            final InvoiceTotals totals = calculate(command.lines());
            database.write(connection -> {
                String sql = "UPDATE CW_INVOICE SET SERVICE_DATE=?,CURRENCY=?,SELLER_NAME=?,SELLER_ADDRESS=?,SELLER_TAX_ID=?,CUSTOMER_NAME=?,CUSTOMER_ADDRESS=?,PAYMENT_TERMS=?,NET_TOTAL=?,TAX_TOTAL=?,GROSS_TOTAL=? WHERE ID=? AND STATUS='DRAFT'";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setDate(1, Date.valueOf(command.serviceDate()));
                    statement.setString(2, command.currency().toUpperCase(Locale.ROOT));
                    statement.setString(3, command.sellerName().trim());
                    statement.setString(4, command.sellerAddress().trim());
                    statement.setString(5, command.sellerTaxId().trim());
                    statement.setString(6, command.customerName().trim());
                    statement.setString(7, command.customerAddress().trim());
                    statement.setString(8, optional(command.paymentTerms()));
                    statement.setBigDecimal(9, totals.net());
                    statement.setBigDecimal(10, totals.tax());
                    statement.setBigDecimal(11, totals.gross());
                    statement.setLong(12, id);
                    if (statement.executeUpdate() != 1)
                        throw updateFailure(connection, id);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM CW_INVOICE_LINE WHERE INVOICE_ID=?")) {
                    statement.setLong(1, id);
                    statement.executeUpdate();
                }
                insertLines(connection, id, totals);
                return null;
            });
            return find(id);
        } catch (InvoiceFailure failure) {
            return ApplicationResult.failure(failure.code, failure.getMessage());
        } catch (IllegalArgumentException validation) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, validation.getMessage());
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice draft could not be updated");
        }
    }

    @Override public ApplicationResult<List<InvoiceDto>> list() {
        try {
            return ApplicationResult.success(database.read(connection -> {
                List<InvoiceDto> invoices = new ArrayList<InvoiceDto>();
                try (Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery("SELECT ID,STATUS,INVOICE_NUMBER,CUSTOMER_NAME,ISSUE_DATE,NET_TOTAL,TAX_TOTAL,GROSS_TOTAL FROM CW_INVOICE ORDER BY CREATED_AT DESC,ID DESC")) {
                    while (rows.next()) invoices.add(summary(rows, 1));
                }
                return invoices;
            }));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoices are unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceDto> find(long id) {
        try {
            InvoiceDto invoice = database.read(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT ID,STATUS,INVOICE_NUMBER,CUSTOMER_NAME,ISSUE_DATE,NET_TOTAL,TAX_TOTAL,GROSS_TOTAL FROM CW_INVOICE WHERE ID=?")) {
                    statement.setLong(1, id);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) return null;
                        return summary(rows, 1);
                    }
                }
            });
            return invoice == null
                    ? ApplicationResult.failure(ApplicationErrorCode.NOT_FOUND, "Invoice does not exist")
                    : ApplicationResult.success(invoice);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice is unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceDetailsDto> details(long id) {
        try {
            InvoiceDetailsDto invoice = database.read(connection -> {
                InvoiceDto summary;
                String sellerName;
                String sellerAddress;
                String sellerTaxId;
                String customerAddress;
                LocalDate serviceDate;
                String currency;
                String paymentTerms;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT ID,STATUS,INVOICE_NUMBER,CUSTOMER_NAME,ISSUE_DATE,NET_TOTAL,TAX_TOTAL,GROSS_TOTAL,SELLER_NAME,SELLER_ADDRESS,SELLER_TAX_ID,CUSTOMER_ADDRESS,SERVICE_DATE,CURRENCY,PAYMENT_TERMS FROM CW_INVOICE WHERE ID=?")) {
                    statement.setLong(1, id);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) return null;
                        summary = summary(rows, 1);
                        sellerName = rows.getString(9);
                        sellerAddress = rows.getString(10);
                        sellerTaxId = rows.getString(11);
                        customerAddress = rows.getString(12);
                        serviceDate = rows.getDate(13).toLocalDate();
                        currency = rows.getString(14).trim();
                        paymentTerms = rows.getString(15);
                    }
                }
                List<InvoiceLineDto> lines = new ArrayList<InvoiceLineDto>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT DESCRIPTION,QUANTITY,UNIT_NET,TAX_RATE,NET_AMOUNT,TAX_AMOUNT,GROSS_AMOUNT FROM CW_INVOICE_LINE WHERE INVOICE_ID=? ORDER BY LINE_NO")) {
                    statement.setLong(1, id);
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) lines.add(new InvoiceLineDto(rows.getString(1), rows.getBigDecimal(2),
                                rows.getBigDecimal(3), rows.getBigDecimal(4), rows.getBigDecimal(5),
                                rows.getBigDecimal(6), rows.getBigDecimal(7)));
                    }
                }
                return new InvoiceDetailsDto(summary, sellerName, sellerAddress, sellerTaxId,
                        customerAddress, serviceDate, currency, paymentTerms, lines);
            });
            return invoice == null
                    ? ApplicationResult.failure(ApplicationErrorCode.NOT_FOUND, "Invoice does not exist")
                    : ApplicationResult.success(invoice);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice details are unavailable");
        }
    }

    @Override public ApplicationResult<InvoiceDto> finalizeDraft(long id, String numberSeries, LocalDate issueDate) {
        try {
            final String series = normalizeSeries(numberSeries);
            if (issueDate == null) throw new IllegalArgumentException("issueDate is required");
            String number = database.write(connection -> {
                String status;
                try (PreparedStatement statement = connection.prepareStatement("SELECT STATUS FROM CW_INVOICE WHERE ID=?")) {
                    statement.setLong(1, id);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) throw new InvoiceFailure(ApplicationErrorCode.NOT_FOUND, "Invoice does not exist");
                        status = rows.getString(1);
                    }
                }
                if (!"DRAFT".equals(status))
                    throw new InvoiceFailure(ApplicationErrorCode.CONFLICT, "Only a draft can be finalized");
                long sequence = nextNumber(connection, series, issueDate.getYear());
                String invoiceNumber = series + "-" + issueDate.getYear() + "-" + String.format(Locale.ROOT, "%06d", sequence);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE CW_INVOICE SET STATUS='FINALIZED',INVOICE_NUMBER=?,NUMBER_SERIES=?,ISSUE_DATE=?,FINALIZED_AT=? WHERE ID=? AND STATUS='DRAFT'")) {
                    statement.setString(1, invoiceNumber);
                    statement.setString(2, series);
                    statement.setDate(3, Date.valueOf(issueDate));
                    statement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                    statement.setLong(5, id);
                    if (statement.executeUpdate() != 1)
                        throw new InvoiceFailure(ApplicationErrorCode.CONFLICT, "Invoice was changed concurrently");
                }
                return invoiceNumber;
            });
            ApplicationResult<InvoiceDto> result = find(id);
            return result.isSuccess() ? result
                    : ApplicationResult.failure(ApplicationErrorCode.INTERNAL, "Finalized invoice " + number + " could not be read");
        } catch (InvoiceFailure failure) {
            return ApplicationResult.failure(failure.code, failure.getMessage());
        } catch (IllegalArgumentException validation) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, validation.getMessage());
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Invoice could not be finalized");
        }
    }

    private static long nextNumber(java.sql.Connection connection, String series, int year) throws Exception {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE CW_INVOICE_SEQUENCE SET NEXT_VALUE=NEXT_VALUE+1 WHERE SERIES_KEY=? AND SEQUENCE_YEAR=?")) {
            update.setString(1, series);
            update.setInt(2, year);
            if (update.executeUpdate() == 0) {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO CW_INVOICE_SEQUENCE (SERIES_KEY,SEQUENCE_YEAR,NEXT_VALUE) VALUES (?,?,2)")) {
                    insert.setString(1, series);
                    insert.setInt(2, year);
                    try {
                        insert.executeUpdate();
                        return 1L;
                    } catch (SQLException concurrentInsert) {
                        if (!"23505".equals(concurrentInsert.getSQLState())) throw concurrentInsert;
                    }
                }
                update.setString(1, series);
                update.setInt(2, year);
                if (update.executeUpdate() != 1)
                    throw new IllegalStateException("Invoice sequence could not be initialized");
            }
        }
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT NEXT_VALUE FROM CW_INVOICE_SEQUENCE WHERE SERIES_KEY=? AND SEQUENCE_YEAR=?")) {
            select.setString(1, series);
            select.setInt(2, year);
            try (ResultSet rows = select.executeQuery()) {
                if (!rows.next()) throw new IllegalStateException("Invoice sequence disappeared");
                return rows.getLong(1) - 1L;
            }
        }
    }

    private static void insertLines(java.sql.Connection connection, long invoiceId, InvoiceTotals totals) throws Exception {
        String sql = "INSERT INTO CW_INVOICE_LINE (INVOICE_ID,LINE_NO,DESCRIPTION,QUANTITY,UNIT_NET,TAX_RATE,NET_AMOUNT,TAX_AMOUNT,GROSS_AMOUNT) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int lineNumber = 0;
            for (CalculatedInvoiceLine line : totals.lines()) {
                statement.setLong(1, invoiceId);
                statement.setInt(2, ++lineNumber);
                statement.setString(3, line.source().description());
                statement.setBigDecimal(4, line.source().quantity());
                statement.setBigDecimal(5, line.source().unitNet());
                statement.setBigDecimal(6, line.source().taxRate());
                statement.setBigDecimal(7, line.net());
                statement.setBigDecimal(8, line.tax());
                statement.setBigDecimal(9, line.gross());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static InvoiceDto summary(ResultSet rows, int offset) throws Exception {
        Date issue = rows.getDate(offset + 4);
        return new InvoiceDto(rows.getLong(offset), rows.getString(offset + 1), rows.getString(offset + 2),
                rows.getString(offset + 3), issue == null ? null : issue.toLocalDate(),
                rows.getBigDecimal(offset + 5), rows.getBigDecimal(offset + 6), rows.getBigDecimal(offset + 7));
    }

    private static InvoiceFailure updateFailure(java.sql.Connection connection, long id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT STATUS FROM CW_INVOICE WHERE ID=?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) return new InvoiceFailure(ApplicationErrorCode.NOT_FOUND, "Invoice does not exist");
                return new InvoiceFailure(ApplicationErrorCode.CONFLICT, "Only a draft can be edited");
            }
        }
    }

    private static void validate(CreateInvoiceCommand command) {
        if (command == null) throw new IllegalArgumentException("invoice is required");
        required(command.sellerName(), "sellerName");
        required(command.sellerAddress(), "sellerAddress");
        required(command.sellerTaxId(), "sellerTaxId");
        required(command.customerName(), "customerName");
        required(command.customerAddress(), "customerAddress");
        if (command.serviceDate() == null) throw new IllegalArgumentException("serviceDate is required");
        if (command.currency() == null || !command.currency().matches("[A-Za-z]{3}"))
            throw new IllegalArgumentException("currency must be a three-letter code");
    }
    private InvoiceTotals calculate(List<InvoiceLineInput> inputs) {
        List<plugins.community.invoice.domain.InvoiceLine> lines =
                new ArrayList<plugins.community.invoice.domain.InvoiceLine>();
        for (InvoiceLineInput input : inputs) {
            if (input == null) throw new IllegalArgumentException("invoice line must not be null");
            lines.add(input.toDomain());
        }
        return calculator.calculate(lines);
    }
    private static String normalizeSeries(String value) {
        String series = required(value, "numberSeries").toUpperCase(Locale.ROOT);
        if (!series.matches("[A-Z0-9][A-Z0-9_-]{0,19}"))
            throw new IllegalArgumentException("numberSeries contains unsupported characters");
        return series;
    }
    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
    private static String optional(String value) { return value == null ? "" : value.trim(); }

    private static final class InvoiceFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final ApplicationErrorCode code;
        private InvoiceFailure(ApplicationErrorCode code, String message) { super(message); this.code = code; }
    }
}
