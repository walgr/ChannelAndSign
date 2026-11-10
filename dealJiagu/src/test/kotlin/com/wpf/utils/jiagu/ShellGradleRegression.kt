package com.wpf.utils.jiagu

import java.io.File
import java.util.Properties

fun main(args: Array<String>) {
    val root = File(args[0])
    val work = File(root, "build/gradle-launch-regression").apply { mkdirs() }
    // Isolate every discovery case from previous runs and real user caches.
    val scratch = java.nio.file.Files.createTempDirectory(work.toPath(), "case-").toFile()
    try {
        val project = File(scratch, "project with spaces").apply { mkdirs() }
        File(project, "gradle/wrapper").mkdirs()
        File(project, "gradle/wrapper/gradle-wrapper.properties").writeText(
            "distributionUrl=https://mirror.invalid/gradle/gradle-8.3-bin.zip\n")
        val home = File(scratch, "selected home")
        val properties = Properties().apply {
            setProperty("user.home", scratch.path)
            setProperty("java.home", System.getProperty("java.home"))
        }
        fun install(hash: String, version: String = "8.3", type: String = "bin", complete: Boolean = true): File {
            val parent = File(home, "wrapper/dists/gradle-$version-$type/$hash").apply { mkdirs() }
            val result = File(parent, "gradle-$version")
            File(result, "lib").mkdirs()
            File(result, "lib/gradle-launcher-$version.jar").writeBytes(byteArrayOf(1))
            if (complete) File(parent, "gradle-$version-$type.zip.ok").writeText("")
            return result
        }
        val env = mapOf("GRADLE_USER_HOME" to home.path)
        fun launch(environment: Map<String, String> = env) = ShellGradle.prepare(project, "", listOf("--version", "--offline"), environment, properties)
        val missing = launch()
        check(missing.command.first().endsWith(if (File.separatorChar == '\\') "gradlew.bat" else "gradlew"))
        install("partial", complete = false)
        install("other-version", version = "8.14")
        check(launch().command.first() == missing.command.first())
        val cached = install("official-url-hash")
        val chosen = launch()
        check(chosen.command.contains(File(cached, "lib/gradle-launcher-8.3.jar").absolutePath))
        check(chosen.command.contains("org.gradle.launcher.GradleMain"))
        check(chosen.command.contains(home.absolutePath))
        check(chosen.environment["GRADLE_USER_HOME"] == home.absolutePath)
        check(chosen.environment["JAVA_HOME"] == File(System.getProperty("java.home")).absolutePath)
        check(chosen.command.any { it == "-Dorg.gradle.java.home=${File(System.getProperty("java.home")).absolutePath}" })
        val explicit = File(scratch, "installed Gradle").apply { File(this, "lib").mkdirs() }
        File(explicit, "lib/gradle-launcher-8.3.jar").writeBytes(byteArrayOf(1))
        check(launch(env + ("GRADLE_HOME" to explicit.path)).command.contains(File(explicit, "lib/gradle-launcher-8.3.jar").absolutePath))
        val all = install("all-url", type = "all")
        File(cached, "lib/gradle-launcher-8.3.jar").delete()
        check(launch().command.contains(File(all, "lib/gradle-launcher-8.3.jar").absolutePath))
        properties.setProperty("gradle.user.home", File(scratch, "property-home").path)
        check(launch().environment["GRADLE_USER_HOME"] == File(scratch, "property-home").absolutePath)
        check(launch().command.first() == missing.command.first())
        println("SHELL_GRADLE_REGRESSION_PASSED: exact version, URL-independent completed cache reuse, bin/all, explicit home, partial-cache rejection, spaces and child JVM/cache settings")
    } finally { scratch.deleteRecursively() }

    // Run the actual installed distribution with a deliberately non-existent mirror URL.
    // --version --offline cannot fetch any build dependency or Gradle distribution.
    val actualProject = java.nio.file.Files.createTempDirectory(work.toPath(), "installed-").toFile()
    try {
        File(actualProject, "gradle/wrapper").mkdirs()
        File(actualProject, "gradle/wrapper/gradle-wrapper.properties").writeText(
            "distributionUrl=https://not-a-real-mirror.invalid/gradle-8.3-bin.zip\n")
        val launch = ShellGradle.prepare(actualProject, "", listOf("--version", "--offline"))
        check("org.gradle.launcher.GradleMain" in launch.command) { "Install/cache Gradle 8.3 to run the local-distribution smoke test" }
        val process = ProcessBuilder(launch.command).directory(actualProject).redirectErrorStream(true).apply {
            environment().putAll(launch.environment)
        }.start()
        val result = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0 && "Gradle 8.3" in result && "Downloading" !in result) { result }
        println(launch.description)
        File(actualProject, "settings.gradle").writeText("rootProject.name = 'local-gradle-regression'\n")
        val build = ShellGradle.prepare(actualProject, "", listOf("help", "--offline", "--no-daemon"))
        val buildProcess = ProcessBuilder(build.command).directory(actualProject).redirectErrorStream(true).apply {
            environment().putAll(build.environment)
        }.start()
        val buildOutput = buildProcess.inputStream.bufferedReader().use { it.readText() }
        check(buildProcess.waitFor() == 0 && "BUILD SUCCESSFUL" in buildOutput && "Downloading" !in buildOutput) { buildOutput }
        println("LOCAL_GRADLE_OFFLINE_PASSED: actual Gradle 8.3 launch without Wrapper or download")
    } finally { actualProject.deleteRecursively() }
}
