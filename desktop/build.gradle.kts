import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Development-only desktop launcher (LWJGL3) for side-by-side testing with keyboard/mouse.
// Not part of the shipped Android app.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
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
    implementation(project(":core"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
}

application {
    mainClass = "ro.klooseduard.streetsofsibiu.desktop.DesktopLauncherKt"
}

tasks.named<JavaExec>("run") {
    // Gdx.files.internal() resolves against the working directory, as on Android it resolves against assets/.
    workingDir = rootDir.resolve("assets")
    // LWJGL warns about JNI on newer JDKs; run the dev build on a JDK 21 LTS toolchain.
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    // Forward dev options, e.g. ./gradlew :desktop:run -Psos.screenshot=/tmp/shot.png
    listOf("sos.window", "sos.screenshot", "sos.screenshotFrame", "sos.screen", "sos.room", "sos.captureRooms").forEach { key ->
        providers.gradleProperty(key).orNull?.let { systemProperty(key, it) }
    }
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}
