package com.wpf.utils.jiagu

import java.io.File

/** Relocate real Java/JNI symbols, not just the Manifest package, so extraction covers every test app. */
internal fun compileMatrixDex(root: File, sdk: File, pkg: String, work: File): Pair<File, File> {
    val classes = File(work, "classes").apply { mkdirs() }
    val source = File(work, "src").apply { mkdirs() }
    val sources = File(root, "dealJiagu/src/test/fixtures/compatibility/com/wpf/jiagu/compat").listFiles()!!
        .filter { it.extension == "java" }.map { original ->
            File(source, original.name).apply { writeText(original.readText(Charsets.UTF_8).replace("com.wpf.jiagu.compat", pkg)) }
        }
    fun run(command: List<String>) {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor() == 0) { "Matrix fixture compilation failed: $output" }
    }
    val android = File(sdk, "platforms/android-33/android.jar")
    val java = File(System.getProperty("java.home"), "bin")
    run(listOf(File(java, "javac.exe").path, "-encoding", "UTF-8", "-source", "8", "-target", "8",
        "-classpath", android.path, "-d", classes.path) + sources.map { it.path })
    val d8 = File(sdk, "build-tools").listFiles()!!.sortedByDescending { it.name }
        .map { File(it, "lib/d8.jar") }.first { it.isFile }
    val primary = File(work, "primary").apply { mkdirs() }
    val secondary = File(work, "secondary").apply { mkdirs() }
    for ((output, isSecondary) in listOf(primary to false, secondary to true)) {
        run(listOf(File(java, "java.exe").path, "-cp", d8.path, "com.android.tools.r8.D8", "--min-api", "21",
            "--lib", android.path, "--output", output.path) + classes.walkTopDown()
            .filter { it.extension == "class" && (it.name == "SecondaryChecks.class") == isSecondary }.map { it.path }.toList())
    }
    return File(primary, "classes.dex") to File(secondary, "classes.dex")
}
