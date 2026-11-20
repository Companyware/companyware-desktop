package plugins.community.mbduscustomer.infrastructure;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;
import platform.api.persistence.PluginDatabase;
import plugins.community.mbduscustomer.application.CustomerDto;
import plugins.community.mbduscustomer.application.CustomerService;

/** Local JDBC adapter; a future remote adapter implements the same application port. */
public final class JdbcCustomerService implements CustomerService {
    private final PluginDatabase database;
    public JdbcCustomerService(PluginDatabase database) { this.database = database; }

    @Override public ApplicationResult<List<CustomerDto>> list() {
        try {
            return ApplicationResult.success(database.read(connection -> {
                List<CustomerDto> rows = new ArrayList<CustomerDto>();
                try (Statement statement = connection.createStatement();
                     ResultSet result = statement.executeQuery("SELECT id, salutation, name, firstname, email, street, streetnumber, zipcode, city, country FROM Customer ORDER BY name, firstname")) {
                    while (result.next()) {
                        String[] fields = new String[9];
                        for (int index = 0; index < fields.length; index++) fields[index] = result.getString(index + 2);
                        rows.add(new CustomerDto(result.getLong(1), fields));
                    }
                }
                return rows;
            }));
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Customer data is unavailable");
        }
    }

    @Override public ApplicationResult<CustomerDto> save(CustomerDto customer) {
        if (customer.field(1).trim().isEmpty()) {
            return ApplicationResult.failure(ApplicationErrorCode.VALIDATION, "Customer name is required");
        }
        try {
            database.write(connection -> {
                String sql = customer.id() == null
                        ? "INSERT INTO Customer (salutation,name,firstname,email,street,streetnumber,zipcode,city,country) VALUES (?,?,?,?,?,?,?,?,?)"
                        : "UPDATE Customer SET salutation=?,name=?,firstname=?,email=?,street=?,streetnumber=?,zipcode=?,city=?,country=? WHERE id=?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    for (int index = 0; index < 9; index++) statement.setString(index + 1, customer.field(index));
                    if (customer.id() != null) statement.setLong(10, customer.id());
                    int changed = statement.executeUpdate();
                    if (customer.id() != null && changed == 0) throw new MissingCustomerException();
                }
                return null;
            });
            return ApplicationResult.success(customer);
        } catch (MissingCustomerException missing) {
            return ApplicationResult.failure(ApplicationErrorCode.NOT_FOUND, "Customer does not exist");
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Customer could not be saved");
        }
    }

    @Override public ApplicationResult<Void> delete(long id) {
        try {
            int changed = database.write(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Customer WHERE id=?")) {
                    statement.setLong(1, id);
                    return statement.executeUpdate();
                }
            });
            return changed == 0
                    ? ApplicationResult.failure(ApplicationErrorCode.NOT_FOUND, "Customer does not exist")
                    : ApplicationResult.success(null);
        } catch (Exception failure) {
            return ApplicationResult.failure(ApplicationErrorCode.UNAVAILABLE, "Customer could not be deleted");
        }
    }

    private static final class MissingCustomerException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
