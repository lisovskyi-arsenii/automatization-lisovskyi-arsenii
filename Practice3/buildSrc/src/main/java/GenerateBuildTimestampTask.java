import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class GenerateBuildTimestampTask extends DefaultTask {
    private static final String TIMESTAMP_FILENAME = "build-timestamp.txt";

    @OutputFile
    public abstract RegularFileProperty getTimestampFile();

    public GenerateBuildTimestampTask() {
        getTimestampFile().convention(
                getProject().getLayout().getBuildDirectory().file(TIMESTAMP_FILENAME)
        );
    }

    @TaskAction
    public void run() throws IOException {
        File timestampFile = getTimestampFile().get().getAsFile();
        File buildDir = timestampFile.getParentFile();

        if (!buildDir.exists()) {
            buildDir.mkdirs();
        }

        LocalDateTime timestamp = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedTimestamp = timestamp.format(formatter);

        try (BufferedWriter writer = Files.newBufferedWriter(timestampFile.toPath())) {
            writer.write("Build Timestamp: " + formattedTimestamp);
        }

        getLogger().lifecycle("Build timestamp generated at: " + timestampFile.getAbsolutePath());
    }
}
