package com.wpf.utils.jiagu

import java.io.File

fun nativeFixtures(root: File, sdk: File, rootsOnly: Boolean = false,
                   packageName: String = "com.wpf.jiagu.compat"): File {
    require(packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")))
    val output = File(root, (if (rootsOnly) "build/native-fixture-roots" else "build/native-fixture") +
        if (packageName == "com.wpf.jiagu.compat") "" else "-$packageName").apply { mkdirs() }
    fun source(name: String): File {
        val original = File(root, "dealJiagu/src/test/fixtures/native/$name")
        if (packageName == "com.wpf.jiagu.compat") return original
        return File(output, "sources/$name").apply {
            parentFile.mkdirs()
            writeText(original.readText().replace("com/wpf/jiagu/compat", packageName.replace('.', '/'))
                .replace("com_wpf_jiagu_compat", packageName.replace('.', '_')))
        }
    }
    val ndk = File(sdk, "ndk/26.3.11579264")
    val clang = File(ndk, "toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe")
    check(clang.isFile) { "Native fixture requires NDK 26.3.11579264" }
    mapOf("x86" to "i686-linux-android21", "x86_64" to "x86_64-linux-android21",
        "armeabi-v7a" to "armv7a-linux-androideabi21", "arm64-v8a" to "aarch64-linux-android21").forEach { (abi, target) ->
        val folder = File(output, abi).apply { mkdirs() }
        for ((name, source) in listOf("dependency" to "dependency.c", "entry" to "entry.c", "standalone" to "standalone.c")) {
            val command = listOf(clang.path, "--target=$target", "-shared", "-fPIC", "-O2", "-Wl,-z,max-page-size=16384",
                "-Wl,-soname,libcompat_$name.so", source(source).path,
                "-o", File(folder, "libcompat_$name.so").path) +
                if (name == "entry") {
                    if (rootsOnly) listOf(source("dependency.c").path)
                    else listOf("-L${folder.path}", "-lcompat_dependency")
                } else emptyList()
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            val log = process.inputStream.bufferedReader().readText(); check(process.waitFor() == 0) { log }
        }
    }
    return output
}
