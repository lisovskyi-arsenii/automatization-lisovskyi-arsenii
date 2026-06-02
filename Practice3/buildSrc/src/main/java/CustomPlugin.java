import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class CustomPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        project.getTasks().register("generateBuildTimestamp", GenerateBuildTimestampTask.class, task -> {
            task.setGroup("customTasks");
            task.setDescription("Generate a timestamp file in the build directory");
        });
        project.getTasks().register("generateChecksum", GenerateChecksumTask.class, task -> {
            task.setGroup("customTasks");
            task.setDescription("Generates checksums for all files in the libs directory");
        });
    }
}
