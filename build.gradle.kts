group = "com.wpf.compose.desktop"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://gitlab.com/api/v4/projects/38224197/packages/maven")
        maven("https://packages.aliyun.com/maven/repository/2428546-release-87ayOu") {
            credentials {
                username = "653b02ee970dc802e532f004"
                password = "ZwOcLGu7St6N"
            }
        }
    }
}

plugins {
    id("com.android.application") version "7.4.2" apply false
    kotlin("multiplatform") apply false
    kotlin("plugin.serialization") apply false
    id("org.jetbrains.compose") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
}
