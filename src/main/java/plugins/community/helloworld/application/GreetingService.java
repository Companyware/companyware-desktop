package plugins.community.helloworld.application;

import platform.api.application.ApplicationResult;

/** Application port; a local or future remote adapter can supply the greeting. */
public interface GreetingService {
    ApplicationResult<String> greeting();
}
