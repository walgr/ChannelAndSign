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
                api(compose.material)
                implementation(project(":base"))
                implementation(project(":WebView"))
                implementation("org.apache.directory.studio:org.apache.commons.codec:1.8")
                implementation("org.bouncycastle:bcprov-jdk18on:1.75")
                implementation("net.dongliu:apk-parser:2.6.10")
                implementation("io.ktor:ktor-client-core:2.3.2")
                implementation("io.ktor:ktor-client-cio:2.3.2")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
                implementation("io.ktor:ktor-client-logging:2.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("com.russhwolf:multiplatform-settings-no-arg:1.0.0")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("com.auth0:java-jwt:4.4.0")
                implementation("com.github.winterreisender:webviewko-jvm:0.6.0")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
    }
}