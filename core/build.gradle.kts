import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    api(libs.gdx.freetype)
    testImplementation(libs.junit)
}

tasks.test {
    systemProperty("sos.repoRoot", rootDir.absolutePath)
}
