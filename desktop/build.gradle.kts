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
//                api("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.77")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            //配置需要的模块
            modules(
                "java.instrument",
                "java.net.http",
                "jdk.jfr",
                "jdk.jsobject",
                "jdk.unsupported",
                "jdk.unsupported.desktop",
                "jdk.xml.dom"
            )
            windows {
                iconFile.set(project.file("icon.png"))
                dirChooser = true
            }
            macOS {
                iconFile.set(project.file("icon.png"))
            }
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "ChannelAndSign"
            packageVersion = "1.0.0"
            description = "by wpf"
        }
    }
}
