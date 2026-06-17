package plugin.util;

import org.apache.maven.project.MavenProject;
import plugin.exception.SourceScanningException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class SourceScanner {
    private SourceScanner() {
        throw new UnsupportedOperationException("Source scanner is utility class - cannot be instantiated");
    }

    public static List<Path> scanSourceFiles(MavenProject project) throws SourceScanningException {
        final List<String> compileSourceRoots = project.getCompileSourceRoots();
        final List<Path> javaFiles = new ArrayList<>();

        try {
            if (!compileSourceRoots.isEmpty()) {
                for (String root : compileSourceRoots) {
                    final Path path = Paths.get(root);
                    if (Files.exists(path)) {
                        javaFiles.addAll(scanDirectory(path));
                    }
                }
            }
        } catch (Exception e) {
            throw new SourceScanningException("Failed to scan directory structures", e);
        }

        return javaFiles;
    }

    private static List<Path> scanDirectory(Path root) throws IOException {
        try (final Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }
}
