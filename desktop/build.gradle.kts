import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.wpf.compose.desktop"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                api(project(":common"))
                api(compose.desktop.currentOs)
                api("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.85.4")
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
            modules(
                "java.compiler",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.net.http",
                "java.prefs",
                "java.security.jgss",
                "java.sql",
                "jdk.jfr",
                "jdk.jsobject",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom"
            )
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "ChannelAndSign"
            packageVersion = "1.0.0"
            description = "by wpf"
        }
    }
}
