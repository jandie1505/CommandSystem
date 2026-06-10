plugins {
    id("java-library")
}

dependencies {
    implementation(project(":Core"))
    implementation(project(":Server"))

    implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")
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
