plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
}

group = "com.wpf.utils"
val versionName = "1.1.20"
version = versionName

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api("com.reandroid.arsc:ARSCLib:1.3.5")
    api("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-cio:3.0.3")
    api("ch.qos.logback:logback-classic:1.5.15")
    api("commons-codec:commons-codec:1.16.0")
    api("com.google.code.gson:gson:2.11.0")
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
            create<MavenPublication>("aliyun") {
                from(components["java"])
                groupId = "com.wpf.utils"
                artifactId = "tools"
                version = versionName
                artifact(tasks["kotlinSourcesJar"])
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