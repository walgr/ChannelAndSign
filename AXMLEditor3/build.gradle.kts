plugins {
    id("kotlin")
}

group = "cn.wjdiankong"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(files("src/main/libs/kxml2-2.3.0.jar"))
    implementation(files("src/main/libs/xmlpull_1_1_3_4c.jar"))
}

kotlin {
    jvmToolchain(8)
}