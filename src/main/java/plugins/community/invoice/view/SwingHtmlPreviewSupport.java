package plugins.community.invoice.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Adapts renderer-owned data images for Swing's legacy HTML viewer. */
final class SwingHtmlPreviewSupport implements AutoCloseable {
    private static final Pattern IMAGE = Pattern.compile(
            "src=\"data:(image/(?:png|jpeg));base64,([^\"]+)\"");
    private Path image;

    String prepare(String html) throws IOException {
        Matcher matcher = IMAGE.matcher(html);
        if (!matcher.find()) {
            close();
            return html;
        }
        byte[] content;
        try {
            content = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Logo data is invalid", invalid);
        }
        close();
        image = Files.createTempFile("companyware-invoice-logo-",
                "image/png".equals(matcher.group(1)) ? ".png" : ".jpg");
        Files.write(image, content);
        image.toFile().deleteOnExit();
        String replacement = "src=\"" + image.toUri().toASCIIString() + "\"";
        return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
    }

    @Override public void close() {
        if (image == null) return;
        try {
            Files.deleteIfExists(image);
        } catch (IOException ignored) {
            image.toFile().deleteOnExit();
        } finally {
            image = null;
        }
    }
}
