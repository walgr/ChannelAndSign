import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
//    id("multiplatform-compose-setup")
}

group = "com.wpf.compose.webview"
version = "1.0-SNAPSHOT"


val os: OperatingSystem = OperatingSystem.current()

val platform = when {
    os.isWindows -> "win"
    os.isMacOsX -> "mac"
    else -> "linux"
}

val jdkVersion = "17"

kotlin {
    jvm("desktop") {
        jvmToolchain(17)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)

            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
                api("org.openjfx:javafx-base:$jdkVersion:${platform}")
                api("org.openjfx:javafx-graphics:$jdkVersion:${platform}")
                api("org.openjfx:javafx-controls:$jdkVersion:${platform}")
                api("org.openjfx:javafx-fxml:$jdkVersion:${platform}")
                api("org.openjfx:javafx-media:$jdkVersion:${platform}")
                api("org.openjfx:javafx-web:$jdkVersion:${platform}")
                api("org.openjfx:javafx-swing:$jdkVersion:${platform}")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
            }
        }
    }
}