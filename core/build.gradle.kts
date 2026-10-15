import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// libGDX rendering, UI, audio, input mapping and screens. Platform-independent.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    api(project(":logic"))
    api(libs.gdx)
    testImplementation(libs.junit)
}

tasks.test {
    // Asset tests compare assets/ with the original Godot project (read-only).
    systemProperty("sos.repoRoot", rootDir.absolutePath)
}
