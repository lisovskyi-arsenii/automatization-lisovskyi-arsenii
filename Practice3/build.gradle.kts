plugins {
    java
    application
    kotlin("jvm") version "2.3.21"
    id("CustomPlugin")
}

group = "org.lisovskyi"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Zip>("packHomeworkIntoZip") {
    group = "homework"
    description = "Packs homework into a zip archive"

    archiveFileName.set("Practice3_LisovskyiArsenii.zip")
    destinationDirectory.set(layout.buildDirectory.dir("homework"))

    from(layout.projectDirectory)

    exclude(
        "build/**",
        ".gradle/**",
        ".idea/**",
        "buildSrc/build/**",
        ".git/**"
    )
}
