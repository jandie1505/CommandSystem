plugins {
    id("java-library")
}

dependencies {
    implementation(project(":Core"))

    implementation("org.json:json:${rootProject.extra["jsonVersion"]}")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
