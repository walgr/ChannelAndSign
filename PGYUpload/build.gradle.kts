plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
    kotlin("plugin.serialization") version "2.1.0"
}

group = "com.wpf.utils"
val versionName = "1.0.0"
version = versionName

dependencies {
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-cio:3.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.15")
    implementation("com.google.code.gson:gson:2.11.0")
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
                artifactId = "pgyupload"
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

tasks.register("打包", Jar::class) {
    group = "jar"
    archiveFileName = "上传蒲公英.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.utils.pgyupload.MainKt"
        attributes["Manifest-Version"] = "1.0.1"
    }
    from(
        sourceSets.main.get().output,
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    )
    exclude(
        "META-INF/*.RSA",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/LICENSE.txt",
        "META-INF/versions/9/module-info.class",
        "module-info.class",
        "META-INF/INDEX.LIST"
    )
}