package pluginmanager;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** One closeable loader for exactly one installed package runtime. */
final class PackagePluginClassLoader extends URLClassLoader {
    PackagePluginClassLoader(Path pluginJar, ClassLoader parent) throws IOException {
        super(new URL[] { pluginJar.toUri().toURL() }, parent);
    }
}
