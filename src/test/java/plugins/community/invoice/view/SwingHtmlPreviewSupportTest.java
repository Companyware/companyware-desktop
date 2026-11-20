package plugins.community.invoice.view;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class SwingHtmlPreviewSupportTest {
    @Test public void materializesAndCleansUpRendererOwnedDataImage() throws Exception {
        SwingHtmlPreviewSupport support = new SwingHtmlPreviewSupport();
        String data = Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3 });
        String prepared = support.prepare("<html><body><img src=\"data:image/png;base64," + data + "\" /></body></html>");
        assertFalse(prepared.contains("data:image/png"));
        Matcher matcher = Pattern.compile("src=\"([^\"]+)\"").matcher(prepared);
        assertTrue(matcher.find());
        Path image = java.nio.file.Paths.get(new URI(matcher.group(1)));
        assertTrue(Files.exists(image));
        support.close();
        assertFalse(Files.exists(image));
    }
}
