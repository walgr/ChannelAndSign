
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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

                implementation(files("src/commonMain/libs/apksigner.jar"))
                implementation(files("src/commonMain/libs/AXMLEditor2.jar"))
                implementation("com.android:zipflinger:8.0.0")      //压缩包操作

//                implementation("co.touchlab:kermit:2.0.0-RC4")          //日志保存
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