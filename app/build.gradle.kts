plugins {
    alias(libs.plugins.android.application)
}

// libGDX ships its Android native libraries (.so) inside per-ABI jars; they are extracted
// into build/gdx-natives/<abi>/ and packaged as jniLibs.
val gdxNativeAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val gdxNativesDir = layout.buildDirectory.dir("gdx-natives")
val gdxNativeConfigurations = gdxNativeAbis.associateWith { abi -> configurations.create("gdxNatives-$abi") }

android {
    namespace = "ro.klooseduard.streetsofsibiu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ro.klooseduard.streetsofsibiu"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets {
        getByName("main") {
            assets.directories.add(rootProject.layout.projectDirectory.dir("assets").asFile.path)
            jniLibs.directories.add(gdxNativesDir.get().asFile.path)
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.android)
    gdxNativeConfigurations.forEach { (abi, configuration) ->
        add(configuration.name, variantOf(libs.gdx.platform) { classifier("natives-$abi") })
    }
    testImplementation(libs.junit)
}

interface ArchiveOperationsHolder {
    @get:Inject
    val archives: ArchiveOperations
}

val copyAndroidNatives = tasks.register<Sync>("copyAndroidNatives") {
    into(gdxNativesDir)
    // Local reference so the lazy provider below does not capture the build script (configuration cache).
    val archives = objects.newInstance<ArchiveOperationsHolder>().archives
    gdxNativeConfigurations.forEach { (abi, configuration) ->
        from(configuration.elements.map { jars -> jars.map { archives.zipTree(it) } }) {
            include("*.so")
            into(abi)
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn(copyAndroidNatives)
}
