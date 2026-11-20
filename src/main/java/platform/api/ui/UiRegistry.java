package platform.api.ui;

import java.util.List;
import platform.api.service.Service;

/** Public contribution registry; implementations serialize mutations on Swing's EDT. */
public interface UiRegistry extends Service {
    AutoCloseable register(UiContributionSpec contribution);
    List<UiContributionSpec> contributions(UiContributionType type);
    AutoCloseable onChange(Runnable listener);
}
