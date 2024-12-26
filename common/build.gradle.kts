plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.wpf.compose.common"
version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop") {
//        jvmToolchain(17)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(project(":base"))
                api(project(":WebView"))
                api(project(":dealJiagu"))
                api("org.apache.directory.studio:org.apache.commons.codec:1.8")
                api("org.bouncycastle:bcprov-jdk18on:1.77")
                api("net.dongliu:apk-parser:2.6.10")
                api("io.ktor:ktor-server-auto-head-response:3.0.3")
                api("io.ktor:ktor-client-core:3.0.3")
                api("io.ktor:ktor-client-cio:3.0.3")
                api("io.ktor:ktor-client-content-negotiation:3.0.3")
                api("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
                api("io.ktor:ktor-client-logging:3.0.3")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.9.0")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                api("com.russhwolf:multiplatform-settings-no-arg:1.1.1")
                api("com.google.code.gson:gson:2.11.0")
                api("com.auth0:java-jwt:4.4.0")
                api("com.fasterxml.jackson.core:jackson-core:2.16.1")
                api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
                api("com.github.winterreisender:webviewko-jvm:0.6.0")
                api("androidx.compose.ui:ui-util-desktop:1.7.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
    }
}