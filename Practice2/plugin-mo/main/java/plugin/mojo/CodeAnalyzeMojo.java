package plugin.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import plugin.exception.SourceScanningException;
import plugin.util.SourceScanner;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mojo(name = "analyze")
public class CodeAnalyzeMojo extends AbstractMojo {
    private static final Pattern TODO_PATTERN = Pattern.compile("//\\s*todo\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIXME_PATTERN = Pattern.compile("//\\s*fixme\\b", Pattern.CASE_INSENSITIVE);
    private static final String METADATA_PATH = "metadata.txt";
    private static final String CODE_ANALYSIS_DATA_PATH = "analysis.txt";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    enum TypeOfComment {
        TODO, FIXME
    }

    record TodoItem (
        TypeOfComment type,
        Path filepath,
        int lineNumber,
        String commentText
    ) {}

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Path targetPath = Paths.get(project.getBuild().getDirectory());
        final Path reportFile = targetPath.resolve(CODE_ANALYSIS_DATA_PATH);
        final Path metadataFile = targetPath.resolve(METADATA_PATH);

        final List<TodoItem> allTodos = new ArrayList<>();

        int totalLines = 0;
        int emptyLines = 0;
        int totalFiles = 0;

        try {
            Files.createDirectories(targetPath);

            final List<Path> javaFiles = SourceScanner.scanSourceFiles(project);
            if (javaFiles.isEmpty()) {
                throw new MojoExecutionException("No source files found");
            }

            for (Path javaFile : javaFiles) {
                final List<String> lines = Files.readAllLines(javaFile);
                final Path filename = javaFile.getFileName();

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    int currentLineNumber = i + 1;

                    totalLines++;
                    if (line.isBlank()) {
                        emptyLines++;
                        continue;
                    }

                    Matcher todoMatcher = TODO_PATTERN.matcher(line);
                    Matcher fixmeMatcher = FIXME_PATTERN.matcher(line);

                    if (todoMatcher.find()) {
                        String message = extractMessage(line, todoMatcher.start());
                        allTodos.add(new TodoItem(TypeOfComment.TODO, filename, currentLineNumber, message));
                    } else if (fixmeMatcher.find()) {
                        String message = extractMessage(line, fixmeMatcher.start());
                        allTodos.add(new TodoItem(TypeOfComment.FIXME, filename, currentLineNumber, message));
                    }
                }
                totalFiles++;
            }

            getLog().info("Found " + allTodos.size() + " todos");

            try (BufferedWriter writer = Files.newBufferedWriter(reportFile)) {
                for (TodoItem item : allTodos) {
                    String recordLine = String.format("[%s] File: %s | Line: %d | Message: %s%n",
                            item.type(),
                            item.filepath().getFileName(),
                            item.lineNumber(),
                            item.commentText()
                    );
                    writer.write(recordLine);
                }
            }

            try (BufferedWriter metaWriter = Files.newBufferedWriter(metadataFile)) {
                metaWriter.write("Total Files: " + totalFiles + "\n");
                metaWriter.write("Total Lines: " + totalLines + "\n");
                metaWriter.write("Empty Lines: " + emptyLines + "\n");
            }

        } catch (SourceScanningException e) {
            throw new MojoExecutionException("Critical scanner failure", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Error during analysis I/O operations", e);
        }


        long fixmeCount = allTodos.stream()
                .filter(item -> item.type() == TypeOfComment.FIXME)
                .count();

        if (fixmeCount > 0) {
            throw new MojoFailureException("Analysis failed: Found " + fixmeCount + " critical FIXME markers");
        }
    }

    private String extractMessage(String line, int commentStartIndex) {
        String commentPart = line.substring(commentStartIndex);
        commentPart = commentPart.substring(2).trim();

        return commentPart.replaceFirst("(?i)^(todo|fixme)\\s*:?\\s*", "").trim();
    }
}
