
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "com.wpf.util"
version = "1.0-SNAPSHOT"

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
                implementation(project(":base"))
                implementation("io.ktor:ktor-client-core:2.3.2")
                implementation("io.ktor:ktor-client-cio:2.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.0.0")        //本地存储
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
    }
}