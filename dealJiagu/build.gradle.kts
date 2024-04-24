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
    implementation("commons-codec:commons-codec:1.16.0")        //md5
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.dongliu:apk-parser:2.6.10")             //获取apk信息
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}