pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("com.android.application") version "7.4.2"
        id("org.jetbrains.kotlin.android") version "1.9.21"
        id("com.android.library") version "7.4.2"
    }
}

//没有安装jdk8需要打开
//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
//}

rootProject.name = "jiaguLibrary"
include("jiagulibrary")

