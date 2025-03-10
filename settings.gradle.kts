pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        kotlin("plugin.serialization").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
        id("com.android.application") version "7.4.2"
        id("com.android.library") version "7.4.2"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    }
}

//没有安装jdk8需要打开
//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
//}

rootProject.name = "ChannelAndSign"

include(":fileReplace")
//include(":AXMLEditor3")
include(":base")
include(":WebView")
include(":common")
include(":desktop")
include(":dealJiagu")
include(":jiagulibrary")
include(":PGYUpload")
include(":Tools")
