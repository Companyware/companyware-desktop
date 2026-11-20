package plugins.community;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import persistence.migration.Migration;
import platform.api.plugin.PluginId;

/** Prevents accidental edits of migrations that existing installations already recorded. */
public class ReferenceMigrationChecksumTest {
    @Test public void helloWorldHistoryRemainsImmutable() throws Exception {
        assertChecksum("plugins/community/helloworld/migration/createTable.sql",
                "d2ca092db0b8ce4e21be966d5d893b0520c5f4b0f5fbdc660cf8885d3dcd8a4d");
        assertChecksum("plugins/community/helloworld/migration/insertGreeting.sql",
                "744b2d1808f1bacfaec2e7c7e0e9c1810be63adb2012d382dd2cd1b0a8e495fc");
    }

    @Test public void customerHistoryRemainsImmutable() throws Exception {
        assertChecksum("plugins/community/mbduscustomer/migration/createCustomer.sql",
                "86ead645d3843c0baa823292a88cc517cddaff3eb3886f5db6565d5e4ed30528");
    }

    @Test public void invoiceHistoryRemainsImmutable() throws Exception {
        assertChecksum("plugins/community/invoice/migration/createInvoiceTables.sql",
                "b6983a94d211a56c1542fa3241a6bcd7a88c9f287a7230fb2f01fbf4558daa33");
        assertChecksum("plugins/community/invoice/migration/createInvoiceTemplateTable.sql",
                "f89fb2280d40532654565953d78c33b6368bf86d704c4e949d29ee042db42455");
        assertChecksum("plugins/community/invoice/migration/createInvoiceBrandingTable.sql",
                "f18d7e93cfba604aace06cba9762a6325c0b934d220e75585566727bc31df820");
        assertChecksum("plugins/community/invoice/migration/addInvoiceLetterheadFields.sql",
                "d67b2e297da11f935515010eec60926d64cc2ffb8200516a7f7565ce5448e6c6");
        assertChecksum("plugins/community/invoice/migration/addInvoiceSmtpFromName.sql",
                "8e34105f9bb5f9ca0b12b10b0536b509c198df39434dcb11f9b326f73cb61502");
    }

    private void assertChecksum(String resource, String expected) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing test resource: " + resource);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int read; (read = input.read(buffer)) != -1;) output.write(buffer, 0, read);
            String sql = new String(output.toByteArray(), StandardCharsets.UTF_8).trim();
            Migration migration = new Migration(PluginId.of("checksum.test"), 1, "checksum", sql);
            assertEquals(resource, expected, migration.checksum());
        }
    }
}
