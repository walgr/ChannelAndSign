plugins {
    id("kotlin")
}

group = "com.wpf.util"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":AXMLEditor3"))
    implementation(files("src/main/libs/apksigner.jar"))
    implementation("com.android:zipflinger:8.0.0")      //压缩包操作
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
//    implementation("co.touchlab:kermit:2.0.0-RC4")          //日志保存
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(8)
}