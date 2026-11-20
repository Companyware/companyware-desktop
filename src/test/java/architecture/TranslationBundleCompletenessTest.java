package architecture;

import static org.junit.Assert.*;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.Test;

/** German and English are release-gated; other language packs may fall back to English. */
public class TranslationBundleCompletenessTest {
    @Test public void everyPluginEnglishBundleHasTheSameGermanKeys() throws Exception {
        try (Stream<Path> files = Files.walk(Paths.get("src/main/java/plugins"))) {
            for (Path english : (Iterable<Path>) files.filter(path -> path.getFileName().toString().equals("messages_en.properties"))::iterator) {
                Path german = english.resolveSibling("messages_de.properties");
                assertTrue("Missing German bundle for " + english, Files.exists(german));
                assertEquals("Translation keys differ in " + english.getParent(), keys(english), keys(german));
            }
        }
    }

    @Test public void everyCoreEnglishBundleHasTheSameGermanKeys() throws Exception {
        try (Stream<Path> files = Files.walk(Paths.get("src/main/resources/i18n"))) {
            for (Path english : (Iterable<Path>) files.filter(path -> path.getFileName().toString().endsWith("_en.properties"))::iterator) {
                String germanName = english.getFileName().toString().replace("_en.properties", "_de.properties");
                Path german = english.resolveSibling(germanName);
                assertTrue("Missing German bundle for " + english, Files.exists(german));
                assertEquals("Translation keys differ in " + english.getParent(), keys(english), keys(german));
            }
        }
    }

    private static java.util.Set<Object> keys(Path path) throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) { properties.load(input); }
        return properties.keySet();
    }
}
