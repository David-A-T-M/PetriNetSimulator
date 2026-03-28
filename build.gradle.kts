plugins {
    id("java")
    id("application")
    id("checkstyle")
    id("com.diffplug.spotless") version "6.25.0"
    id("jacoco")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

application {
    mainClass.set("ar.edu.unc.david.petrinetsimulator.Main")
}

group = "ar.edu.unc.david.petrinetsimulator"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    // Testing Framework
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mocking for tests
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testImplementation("org.mockito:mockito-core:5.11.0")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.1")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.17.0")

    // Annotations
    implementation("org.jetbrains:annotations:24.0.0")
}

tasks.test {
    useJUnitPlatform()
}

checkstyle {
    toolVersion = "10.12.1"
    configFile = file("${rootProject.projectDir}/checkstyle.xml")
    isIgnoreFailures = false
    configProperties = mapOf("org.checkstyle.google.suppressionfilter.config" to "")
}

tasks.check {
    dependsOn(tasks.named("checkstyleMain"))
    dependsOn(tasks.named("checkstyleTest"))
}

spotless {
    java {
        googleJavaFormat("1.19.1")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        target("src/**/*.java")
    }
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) {
            exclude("ar/edu/unc/david/petrinetsimulator/Main*")
        }
    }))
}