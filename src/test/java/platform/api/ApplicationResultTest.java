package platform.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import platform.api.application.ApplicationErrorCode;
import platform.api.application.ApplicationResult;

public class ApplicationResultTest {
    @Test public void successCarriesAValueWithoutAnError() {
        ApplicationResult<String> result = ApplicationResult.success("value");
        assertTrue(result.isSuccess());
        assertEquals("value", result.valueOrThrow());
        assertFalse(result.errorCode().isPresent());
    }

    @Test public void failureCarriesStableCodeAndSafeMessage() {
        ApplicationResult<String> result = ApplicationResult.failure(
                ApplicationErrorCode.UNAVAILABLE, "temporarily unavailable");
        assertFalse(result.isSuccess());
        assertEquals(ApplicationErrorCode.UNAVAILABLE, result.errorCode().get());
        assertEquals("temporarily unavailable", result.message());
    }
}
