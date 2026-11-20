package plugins.community.companywaredemo.application;

import java.util.List;
import platform.api.application.ApplicationResult;

/** Read-use-case port independent of Swing, JDBC and a transport protocol. */
public interface DemoOverviewService {
    ApplicationResult<List<DemoRow>> listRows();
}
