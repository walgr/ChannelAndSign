import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.wpf.compose.desktop"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
    jvm {
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":common"))
                api(compose.desktop.currentOs)
                api("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.8.18")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        buildTypes.release.proguard {
            configurationFiles.from("rules.pro")
        }
        nativeDistributions {
            //配置需要的模块
            modules("java.instrument", "java.management", "java.naming", "java.prefs", "java.sql", "jdk.unsupported")
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "ChannelAndSign"
            packageVersion = "1.0.0"
            description = "by wpf"
        }
    }
}
