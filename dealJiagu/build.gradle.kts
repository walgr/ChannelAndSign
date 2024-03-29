plugins {
    id("kotlin")
}

group = "com.wpf.utils"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.wpf.utils:tools:+")
    implementation("com.android:zipflinger:7.3.1")              //压缩包操作
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}