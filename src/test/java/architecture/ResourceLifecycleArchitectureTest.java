package architecture;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class ResourceLifecycleArchitectureTest {
    private static final Pattern OPEN_SESSION = Pattern.compile("getSessionFactory\\(\\)\\.openSession\\(\\)");
    private static final Pattern OWNED_SESSION = Pattern.compile(
            "Session\\s+session\\s*=\\s*HibernateUtils\\.getSessionFactory\\(\\)\\.openSession\\(\\)",
            Pattern.MULTILINE);

    @Test public void everyHibernateSessionIsLexicallyOwnedByTryWithResources() throws Exception {
        Path root = Paths.get("src/main/java");
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                try {
                    String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    assertTrue(path + " has an unmanaged Hibernate Session",
                            matches(OPEN_SESSION, source) == matches(OWNED_SESSION, source));
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
        }
    }

    private static int matches(Pattern pattern, String source) {
        int count = 0;
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) count++;
        return count;
    }
}
