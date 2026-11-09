import java.util.Properties

plugins {
    id("kotlin")
}

group = "com.wpf.utils"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.wpf.utils:tools:1.1.18")
    implementation("com.android:zipflinger:7.3.1")              //Apk操作
    implementation("net.lingala.zip4j:zip4j:2.11.5")            //压缩包操作
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("net.dongliu:apk-parser:2.6.10")             //获取apk信息
    implementation("org.smali:dexlib2:2.5.2") // Rebuild selected D2C methods as native DEX declarations.
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
    from("../jiagulibrary") {
        include("app/**")
        into("")
    }
    from("../gradle") {
        into("gradle")
    }
    from(
        "../jiagulibrary/settings.gradle.kts",
        "../jiagulibrary/build.gradle.kts",
        "../jiagulibrary/local.properties",
        "../jiagulibrary/gradle.properties",
        "../jiagulibrary/jiagu.jks",
        "../gradlew",
        "../gradlew.bat",
    )
}

tasks.register("加固打包", Jar::class) {
    group = "jiagu"
    dependsOn("zipJiaguLibrary")
    archiveFileName = "加固.jar"
    destinationDirectory.set(file("D:\\Android\\ShareFile\\tools"))
    manifest {
        attributes["Main-Class"] = "com.wpf.utils.jiagu.MainKt"
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
        "META-INF/NOTICE.txt",
        "META-INF/versions/9/module-info.class",
        "module-info.class",
        "META-INF/INDEX.LIST"
    )
}

// JiaGu loads the shell sources from this resource, including in desktop builds.
tasks.named("processResources") {
    dependsOn("zipJiaguLibrary")
}

tasks.register<JavaExec>("verifyAesPayload") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.utils.AES128HelperRegressionKt")
}

val compileD2cFixture = tasks.register<JavaCompile>("compileD2cFixture") {
    source("src/test/fixtures/d2c")
    classpath = files()
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("d2c/classes"))
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

val prepareD2cFixture = tasks.register<JavaExec>("prepareD2cFixture") {
    dependsOn(compileD2cFixture)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    mainClass.set("com.android.tools.r8.D8")
    val dexOutput = rootProject.layout.buildDirectory.dir("d2c/original")
    outputs.dir(dexOutput)
    inputs.files(compileD2cFixture.map { it.destinationDirectory })
    doFirst {
        val properties = Properties()
        rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { properties.load(it) }
        val sdk = file(properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
            ?: error("D2C fixture requires sdk.dir in local.properties or ANDROID_HOME"))
        val d8 = sdk.resolve("build-tools").listFiles()?.sortedByDescending { it.name }
            ?.map { it.resolve("lib/d8.jar") }?.firstOrNull { it.isFile }
            ?: error("Install Android SDK build-tools to run D2C fixture")
        val androidJar = sdk.resolve("platforms/android-33/android.jar")
        check(androidJar.isFile) { "D2C fixture requires Android SDK platform 33" }
        dexOutput.get().asFile.mkdirs()
        classpath = files(d8)
        setArgs(listOf("--min-api", "26", "--lib", androidJar.path, "--output", dexOutput.get().asFile.path) +
            compileD2cFixture.get().destinationDirectory.get().asFile.walkTopDown()
                .filter { it.extension == "class" }.map { it.path }.toList())
    }
}

tasks.register<JavaExec>("verifyD2c") {
    group = "verification"
    dependsOn("testClasses", prepareD2cFixture)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.d2c.D2cRegressionKt")
    args(rootProject.file("build/d2c/original/classes.dex").path, rootProject.file("build/d2c/generated").path)
}
