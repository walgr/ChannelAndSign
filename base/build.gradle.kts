plugins {
    id("kotlin")
}

group = "com.wpf.util"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("src/main/libs/apksigner.jar"))
    implementation(files("src/main/libs/AXMLEditor2.jar"))
    implementation("com.android:zipflinger:8.0.0")      //压缩包操作
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(8)
}