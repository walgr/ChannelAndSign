pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("multiplatform").version(extra["kotlin.version"] as String)
        kotlin("plugin.serialization").version(extra["kotlin.version"] as String)
        id("org.jetbrains.compose").version(extra["compose.version"] as String)
    }
}

rootProject.name = "ChannelAndSign"

include(":fileReplace")
include(":AXMLEditor3")
include(":base")
include(":WebView")
include(":common")
include(":desktop")
include(":FileService")