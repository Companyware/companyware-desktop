package plugins.community.mbduscustomer.application;

import java.util.List;
import platform.api.application.ApplicationResult;

/** Customer use cases independent of Swing, JDBC and the eventual transport. */
public interface CustomerService {
    ApplicationResult<List<CustomerDto>> list();
    ApplicationResult<CustomerDto> save(CustomerDto customer);
    ApplicationResult<Void> delete(long id);
}
