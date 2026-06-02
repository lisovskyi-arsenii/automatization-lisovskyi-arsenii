import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class GenerateChecksumTask extends DefaultTask {
    private static final String LIBS_DIRECTORY_NAME = "libs";
    private static final String MD_ALGORITHM = "SHA-256";

    @InputDirectory
    @Optional
    public abstract DirectoryProperty getLibsDirectory();

    public GenerateChecksumTask() {
        getLibsDirectory().convention(
            getProject().getLayout().getBuildDirectory().dir(LIBS_DIRECTORY_NAME)
        );
    }

    @TaskAction
    public void run() throws IOException, NoSuchAlgorithmException {
        File libsDirectory = getLibsDirectory().get().getAsFile();

        if (!libsDirectory.exists()) {
            getLogger().lifecycle("No libs directory found at: " + libsDirectory.getAbsolutePath());
            getLogger().lifecycle("Run 'build' task first");
            return;
        }

        File[] jarFiles = libsDirectory.listFiles((_, name) -> name.endsWith(".jar"));

        if (jarFiles == null || jarFiles.length == 0) {
            getLogger().lifecycle("No JAR files found in: " + libsDirectory.getAbsolutePath());
            return;
        }

        MessageDigest messageDigest = MessageDigest.getInstance(MD_ALGORITHM);

        getLogger().lifecycle("Generating " + MD_ALGORITHM + " checksums for JAR files:");

        for (File jarFile : jarFiles) {
            try (FileInputStream inputStream = new FileInputStream(jarFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }

                byte[] digest = messageDigest.digest();
                String checksum = bytesToHex(digest);

                getLogger().lifecycle("  " + jarFile.getName() + ": " + checksum);
            }
        }

        getLogger().lifecycle("Total: " + jarFiles.length + " JAR file(s)");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
