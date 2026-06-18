plugins {
    id("java")
    id("jacoco")
    id("info.solidsoft.pitest") version "1.19.0"
}

group = "sample.plugin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jacocoVersion       = "0.8.15"
val pitestToolVersion       = "1.25.4"
val pitestJUnitVersion  = "1.2.3"
val assertJVersion      = "4.0.0-M1"
val mockitoVersion      = "5.23.0"
val lombokVersion       = "1.18.46"

dependencies {
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

pitest {
    junit5PluginVersion.set(pitestJUnitVersion)
    pitestVersion.set(pitestToolVersion)
    targetClasses.set(setOf(
        "service.*",
        "util.*"
    ))
    mutators.set(setOf("DEFAULTS"))
    outputFormats.set(setOf("HTML", "XML"))
}

jacoco {
    toolVersion = jacocoVersion
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(true)
    }
}