plugins {
    `java-gradle-plugin`
}

gradlePlugin {
    plugins {
        create("myCustomPlugin") {
            id = "CustomPlugin"
            implementationClass = "CustomPlugin"
        }
    }
}

repositories {
    mavenCentral()
}
