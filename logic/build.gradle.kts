import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin game logic: no libGDX, no Android. Unit-tested on the JVM.
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
    testImplementation(libs.junit)
}

tasks.test {
    // Parity tests read the original Godot project (read-only) from the repository root.
    systemProperty("sos.repoRoot", rootDir.absolutePath)
}
