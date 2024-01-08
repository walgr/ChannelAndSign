plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "com.wpf.compose.common"
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
                api(compose.material3)
                api(project(":base"))
                api(project(":WebView"))
                api(project(":FileService"))
                api("org.apache.directory.studio:org.apache.commons.codec:1.8")
                api("org.bouncycastle:bcprov-jdk18on:1.75")
                api("net.dongliu:apk-parser:2.6.10")
                api("io.ktor:ktor-server-auto-head-response:2.3.7")
                api("io.ktor:ktor-client-core:2.3.7")
                api("io.ktor:ktor-client-cio:2.3.7")
                api("io.ktor:ktor-client-content-negotiation:2.3.7")
                api("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                api("io.ktor:ktor-client-logging:2.3.7")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                api("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
                api("com.google.code.gson:gson:2.10.1")
                api("com.auth0:java-jwt:4.4.0")
                api("com.github.winterreisender:webviewko-jvm:0.6.0")
                api("androidx.compose.ui:ui-util-desktop:1.6.0-beta02")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
    }
}