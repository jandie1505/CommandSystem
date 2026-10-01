plugins {
    id("java")
    id("application")
}

dependencies {
    implementation(project(":Core"))
    implementation(project(":Tools"))
    implementation(project(":JLine"))

    implementation("org.slf4j:slf4j-api:${rootProject.extra["slf4jVersion"]}")
    implementation("org.jline:jline:${rootProject.extra["jLineVersion"]}")
    compileOnly("org.jetbrains:annotations:26.1.0")

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("net.jandie1505.commandsystem.demo.DemoApplication")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
