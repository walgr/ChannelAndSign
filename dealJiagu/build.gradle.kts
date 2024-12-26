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
    implementation("com.android:zipflinger:8.7.3")              //Apk操作
    implementation("net.lingala.zip4j:zip4j:2.11.5")            //压缩包操作
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("net.dongliu:apk-parser:2.6.10")             //获取apk信息
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    jvmToolchain(8)
}

tasks.register("zipJiaguLibrary", Zip::class) {
    group = "jiagu"
    archiveFileName = "jiaguLibrary.zip"
    destinationDirectory = layout.projectDirectory.dir("src/main/resources")
    from("../jiagulibrary") {
        include("src/**", "build.gradle")
        into("jiagulibrary")
    }
    from(
        "../gradle") {
        into("gradle")
    }
    from(
        "../jiagulibrary/settings.gradle.kts",
        "../jiagulibrary/build.gradle.kts",
        "../jiagulibrary/local.properties",
        "../gradlew",
        "../gradle.properties",
        "../gradlew.bat"
    )
}

task("打包", Jar::class) {
    group = "jiagu"
//    dependsOn("zipJiaguLibrary")
    archiveFileName = "加固.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.utils.jiagu.MainKt"
        attributes["Manifest-Version"] = "1.0.0"
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
