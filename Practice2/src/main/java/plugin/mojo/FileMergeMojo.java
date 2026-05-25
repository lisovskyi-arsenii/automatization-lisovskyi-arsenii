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
import java.util.List;

@Mojo(name = "merge")
public class FileMergeMojo extends AbstractMojo {
    private static final String END_OF_FILE = "\n";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path targetDir = Paths.get(project.getBuild().getDirectory());
        Path mergedCodeFile = targetDir.resolve("merged_code.txt");

        try {
            Files.createDirectories(targetDir);
            List<Path> javaFiles = SourceScanner.scanSourceFiles(project);

            if (javaFiles.isEmpty()) {
                throw new MojoFailureException("Aggregation failed: No java source files found in the project");
            }

            try (BufferedWriter writer = Files.newBufferedWriter(mergedCodeFile)) {
                for (Path javaFile : javaFiles) {
                    writer.write("\n// ==================== " + javaFile.getFileName() + " ====================\n");

                    String allCodeFromFile = Files.readString(javaFile);
                    writer.write(allCodeFromFile);
                    writer.write(END_OF_FILE);
                }
            }

            getLog().info("Merged code written to " + mergedCodeFile);
        } catch (SourceScanningException e) {
            throw new MojoExecutionException("Critical scanner failure", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed writing merged code output", e);
        }
    }
}
