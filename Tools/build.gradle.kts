plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

group = "com.wpf.utils"
val versionName = "1.1.15"
version = versionName

dependencies {
    api("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    api("ch.qos.logback:logback-classic:1.2.13")
    api("commons-codec:commons-codec:1.16.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}

afterEvaluate {
    publishing {
        publications {
            register("aliyun", MavenPublication::class.java) {
                from(components["java"])
                groupId = "com.wpf.utils"
                artifactId = "tools"
                version = versionName
            }
        }
        repositories {
            maven("https://packages.aliyun.com/maven/repository/2428546-release-87ayOu") {
                credentials {
                    username = "653b02ee970dc802e532f004"
                    password = "ZwOcLGu7St6N"
                }
            }
        }
    }
}