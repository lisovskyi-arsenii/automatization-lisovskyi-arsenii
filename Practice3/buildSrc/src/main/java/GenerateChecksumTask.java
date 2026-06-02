import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GenerateChecksumTask extends DefaultTask {
    public enum State {
        MODIFIED,
        ADDED,
        REMOVED,
        UNCHANGED
    }

    private static final String MAIN_DIRECTORY_NAME = "src/main/";
    private static final String SOURCE_DIRECTORY_NAME_JAVA = MAIN_DIRECTORY_NAME + "java";
    private static final String SOURCE_DIRECTORY_NAME_KOTLIN = MAIN_DIRECTORY_NAME + "kotlin";
    private static final String MD_ALGORITHM = "SHA-256";
    private static final String CHECKSUM_DIRECTORY_NAME = "checksum";
    private static final String PREVIOUS_CHECKSUM_FILE = "previous-checksums.txt";
    private static final int EOF = -1;

    @InputDirectory
    @Optional
    public abstract DirectoryProperty getJavaSourceDirectory();

    @InputDirectory
    @Optional
    public abstract DirectoryProperty getKotlinSourceDirectory();

    @OutputDirectory
    public abstract DirectoryProperty getChecksumDirectory();

    public GenerateChecksumTask() {
        var projectDir = getProject().getLayout().getProjectDirectory();

        File javaDir = projectDir.dir(SOURCE_DIRECTORY_NAME_JAVA).getAsFile();
        if (javaDir.exists()) {
            getJavaSourceDirectory().set(projectDir.dir(SOURCE_DIRECTORY_NAME_JAVA));
        }

        File kotlinDir = projectDir.dir(SOURCE_DIRECTORY_NAME_KOTLIN).getAsFile();
        if (kotlinDir.exists()) {
            getKotlinSourceDirectory().set(projectDir.dir(SOURCE_DIRECTORY_NAME_KOTLIN));
        }

        getChecksumDirectory().convention(
                getProject().getLayout().getBuildDirectory().dir(CHECKSUM_DIRECTORY_NAME)
        );
    }

    @TaskAction
    public void run() throws IOException, NoSuchAlgorithmException {
        File javaDirectory = getJavaSourceDirectory().isPresent() ? getJavaSourceDirectory().get().getAsFile() : null;
        File kotlinDirectory = getKotlinSourceDirectory().isPresent() ? getKotlinSourceDirectory().get().getAsFile() : null;
        File outputDirectory = getChecksumDirectory().get().getAsFile();

        boolean hasJava = javaDirectory != null && javaDirectory.exists();
        boolean hasKotlin = kotlinDirectory != null && kotlinDirectory.exists();

        if (!hasJava && !hasKotlin) {
            getLogger().lifecycle("No source directories found. Expected: src/main/java and/or src/main/kotlin");
            return;
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        List<File> sourceFiles = new ArrayList<>();

        if (hasJava) {
            try (var stream = Files.walk(javaDirectory.toPath())) {
                stream.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> sourceFiles.add(path.toFile()));
            }
        }

        if (hasKotlin) {
            try (var stream = Files.walk(kotlinDirectory.toPath())) {
                stream.filter(path -> path.toString().endsWith(".kt"))
                        .forEach(path -> sourceFiles.add(path.toFile()));
            }
        }

        if (sourceFiles.isEmpty()) {
            getLogger().lifecycle("No source files found (.java or .kt)");
            return;
        }

        getLogger().lifecycle("Generating " + MD_ALGORITHM + " checksums for source files:");

        File previousChecksumFile = new File(outputDirectory, PREVIOUS_CHECKSUM_FILE);
        Map<String, String> prevChecksums = readPreviousChecksums(previousChecksumFile);
        Map<String, String> currentChecksums = new HashMap<>();

        for (File file : sourceFiles) {
            MessageDigest messageDigest = MessageDigest.getInstance(MD_ALGORITHM);

            try (FileInputStream inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != EOF) {
                    messageDigest.update(buffer, 0, bytesRead);
                }

                byte[] digest = messageDigest.digest();
                String checksum = bytesToHex(digest);

                File checksumFile = new File(outputDirectory, file.getName() + ".sha256");

                try (BufferedWriter writer = Files.newBufferedWriter(checksumFile.toPath())) {
                    writer.write(file.getName() + ": " + checksum);
                }

                currentChecksums.put(file.getPath(), checksum);
            }
        }

        for (var entry : currentChecksums.entrySet()) {
            String filepath = entry.getKey();
            String newChecksum = entry.getValue();

            String filename = new File(filepath).getName();

            if (!prevChecksums.containsKey(filepath)) {
                getLogger().lifecycle("  " + filename + " - " + State.ADDED);
            } else if (!prevChecksums.get(filepath).equals(newChecksum)) {
                getLogger().lifecycle("  " + filename + " - " + State.MODIFIED);
            } else {
                getLogger().lifecycle("  " + filename + " - " + State.UNCHANGED);
            }
        }

        for (String fileName : prevChecksums.keySet()) {
            if (!currentChecksums.containsKey(fileName)) {
                getLogger().lifecycle("  " + fileName + " - " + State.REMOVED);
            }
        }

        saveChecksums(previousChecksumFile, currentChecksums);
        getLogger().lifecycle("Total: " + sourceFiles.size() + " source file(s)");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private Map<String, String> readPreviousChecksums(final File checksumFile) throws IOException {
        Map<String, String> prev = new HashMap<>();
        if (checksumFile.exists()) {
            List<String> lines = Files.readAllLines(checksumFile.toPath());
            for (String line : lines) {
                String[] parts = line.split(": ", 2);
                if (parts.length == 2) {
                    prev.put(parts[0].trim(), parts[1].trim());
                }
            }
        }

        return prev;
    }

    private void saveChecksums(final File checksumFile, final Map<String, String> checksums) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(checksumFile.toPath())) {
            for (Map.Entry<String, String> entry : checksums.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
        }
    }
}
