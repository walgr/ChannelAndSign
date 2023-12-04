plugins {
    id("kotlin")
}

group = "com.wpf.util"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("src/main/libs/APKParser.jar"))
//    implementation(project(":AXMLEditor3"))
    implementation("com.android:zipflinger:7.3.1")              //压缩包操作
//    implementation("co.touchlab:kermit:2.0.0-RC4")            //日志保存
    implementation(kotlin("stdlib-jdk8"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}