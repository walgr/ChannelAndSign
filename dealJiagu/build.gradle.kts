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

// Exercise the exact Android verifier source on the JVM, without an Android framework stub.
sourceSets.named("test") {
    java.srcDir("../jiagulibrary/src/main/java")
    java.include("**/IntegrityVerifier.java", "**/RuntimeCompatibility.java")
}

tasks.register<JavaExec>("verifyShellIntegrity") {
    group = "verification"
    dependsOn("testClasses")
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.IntegrityRegressionKt")
    args(rootProject.projectDir.path)
}

tasks.register<JavaExec>("verifyShellGradle") {
    group = "verification"
    dependsOn("testClasses")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.ShellGradleRegressionKt")
    args(rootProject.projectDir.path)
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

tasks.register<JavaExec>("verifyXapk") {
    group = "verification"
    dependsOn("testClasses", "prepareHybridFixture")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.XapkRegressionKt")
    doFirst { setArgs(listOf(rootProject.projectDir.path, fixtureSdk().path) +
        if (project.hasProperty("xapkHarden") || gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "verifyCompatibility" })
            listOf("--harden") else emptyList()) }
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

val compileHybridFixture = tasks.register<JavaCompile>("compileHybridFixture") {
    source("src/test/fixtures/d2c", "src/test/fixtures/hybrid-app", "src/test/fixtures/compatibility")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("hybrid-app/classes"))
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

val compileHybridShellFixture = tasks.register<JavaCompile>("compileHybridShellFixture") {
    source("../jiagulibrary/src/main/java")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("hybrid-app/shell-classes"))
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

// Keep the integration fixture independent of manually prepared provider-shell files.
fun fixtureSdk(): File {
    val properties = Properties()
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { properties.load(it) }
    return file(properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
        ?: error("Hybrid fixture requires sdk.dir in local.properties or ANDROID_HOME"))
}

listOf(compileHybridFixture, compileHybridShellFixture).forEach { compile ->
    compile.configure { classpath = files(provider { fixtureSdk().resolve("platforms/android-33/android.jar") }) }
}

val hybridDexFixtures = listOf(
    Triple("prepareHybridFixture", compileHybridFixture, "hybrid-app/original"),
    Triple("prepareHybridShellFixture", compileHybridShellFixture, "hybrid-app/shell")
).map { (name, compile, outputPath) ->
    tasks.register<JavaExec>(name) {
        dependsOn(compile)
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
        mainClass.set("com.android.tools.r8.D8")
        val dexOutput = rootProject.layout.buildDirectory.dir(outputPath)
        inputs.files(compile.map { it.destinationDirectory })
        outputs.dir(dexOutput)
        doFirst {
            val sdk = fixtureSdk()
            val d8 = sdk.resolve("build-tools").listFiles()?.sortedByDescending { it.name }
                ?.map { it.resolve("lib/d8.jar") }?.firstOrNull { it.isFile }
                ?: error("Install Android SDK build-tools to run hybrid fixture")
            val androidJar = sdk.resolve("platforms/android-33/android.jar")
            check(androidJar.isFile) { "Hybrid fixture requires Android SDK platform 33" }
            dexOutput.get().asFile.mkdirs()
            classpath = files(d8)
            setArgs(listOf("--min-api", "26", "--lib", androidJar.path, "--output", dexOutput.get().asFile.path) +
                compile.get().destinationDirectory.get().asFile.walkTopDown()
                    .filter { it.extension == "class" }.map { it.path }.toList())
        }
    }
}

tasks.register<JavaExec>("verifyHybridPayload") {
    group = "verification"
    dependsOn("testClasses", hybridDexFixtures)
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.d2c.HybridPayloadRegressionKt")
    args(rootProject.file("build/hybrid-app/original/classes.dex").path,
        rootProject.file("build/hybrid-app/shell/classes.dex").path,
        rootProject.file("build/hybrid-app/payload").path)
}

val compatibilityChecks = listOf("verifyD2c", "verifyAesPayload", "verifyHybridPayload", "verifyShellIntegrity", "verifyShellGradle", "verifyXapk")
val compatibilityReport = tasks.register("writeCompatibilityReport") {
    group = "verification"
    mustRunAfter(compatibilityChecks)
    doLast {
        val results = compatibilityChecks.map { name ->
            val check = tasks.named(name).get()
            mapOf("task" to check.path, "status" to when {
                check.state.failure != null -> "failed"
                !check.state.executed -> "not_run"
                check.state.skipped -> "skipped"
                else -> "passed"
            })
        }
        val folder = rootProject.file("build/compatibility").apply { mkdirs() }
        val status = if (results.any { it["status"] == "failed" }) "failed"
            else if (results.all { it["status"] == "passed" }) "passed" else "incomplete"
        val report = mapOf("schemaVersion" to 1, "scope" to "host_and_packaging", "status" to status,
            "checks" to results, "deviceValidation" to "not_run", "startupPerformance" to "not_measured",
            "note" to "Host/runtime-contract tests do not prove device/ABI/OEM startup compatibility; run verifyDeviceCompatibility for a selected device")
        folder.resolve("host-results.json").writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(report)))
        folder.resolve("host-results.md").writeText("# 自动兼容性回归（构建侧）\n\n状态：$status\n\n" +
            results.joinToString("\n") { "- ${it["task"]}：${it["status"]}" } +
            "\n\n设备验证：未执行。此结果不代表所有 Android/ABI/OEM 均已通过启动验证。\n")
        println("兼容性回归报告：${folder.resolve("host-results.json")}")
    }
}
tasks.register("verifyCompatibility") {
    group = "verification"
    description = "Run all host regressions and full XAPK hardening/signing; write an explicit evidence summary"
    dependsOn(compatibilityChecks)
    finalizedBy(compatibilityReport)
}
compatibilityChecks.forEach { name -> tasks.named(name).configure { finalizedBy(compatibilityReport) } }

val prepareCompatibilityApp = tasks.register<JavaExec>("prepareCompatibilityApp") {
    group = "verification"
    dependsOn("testClasses", "prepareHybridFixture")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.CompatibilityFixtureKt")
    doFirst { setArgs(listOf(rootProject.projectDir.path, fixtureSdk().path)) }
}
tasks.register<JavaExec>("verifyDeviceCompatibility") {
    group = "verification"
    dependsOn(prepareCompatibilityApp)
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.wpf.utils.jiagu.DeviceCompatibilityKt")
    doFirst {
        val serial = providers.gradleProperty("deviceSerial").orNull
            ?: error("Provide -PdeviceSerial=<adb serial> for an explicitly selected test device")
        setArgs(listOf(rootProject.projectDir.path, serial))
    }
}
