package com.wpf.utils.jiagu

import com.wpf.util.jiagulibrary.utils.NativeArchive
import com.wpf.util.jiagulibrary.utils.IntegrityVerifier
import java.io.File
import java.util.zip.*

fun main(args: Array<String>) {
    val root = File(args[0]); val work = File(root, "build/native-regression").apply { mkdirs() }
    val libs = nativeFixtures(root, File(args[1])); val key = NativeProtection.key("0123456789abcdef")
    check(key != ResourceEncryption.key("0123456789abcdef"))
    val entries = linkedMapOf("AndroidManifest.xml" to byteArrayOf(1), "classes.dex" to byteArrayOf(2), "resources.arsc" to byteArrayOf(3))
    libs.walkTopDown().filter { it.extension == "so" }.forEach { entries["lib/" + it.relativeTo(libs).invariantSeparatorsPath] = it.readBytes() }
    fun write(apk: File, contents: Map<String, ByteArray>) = ZipOutputStream(apk.outputStream()).use { out ->
        contents.forEach { (name, bytes) -> out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry() }
    }
    val markers = libs.listFiles()!!.associate { it.name to File(it, "libcompat_standalone.so") }
    val apk = File(work, "test.apk"); write(apk, entries)
    val result = NativeProtection.protect(apk, true, key, markers)
    check(result.encryptedFiles.size == 8 && result.retainedFiles.size == 4)
    check(result.retainedFiles.all { it.key.endsWith("libcompat_dependency.so") && "DT_NEEDED" in it.value })
    val signing = IntegritySeal.key(File(work, "key")); IntegritySeal.seal(apk, signing)
    IntegrityVerifier.verify(apk, IntegritySeal.publicHex(signing)); val version = requireNotNull(IntegrityVerifier.nativeVersion())
    for (abi in markers.keys) {
        val restored = requireNotNull(NativeArchive.restore(apk, File(work, "cache-${System.nanoTime()}"), version, abi, key))
        check(!restored.cacheHit)
        check(File(restored.directory, "libcompat_entry.so").readBytes().contentEquals(entries.getValue("lib/$abi/libcompat_entry.so")))
        check(!File(restored.directory, "libcompat_dependency.so").exists())
        val cache = restored.directory.parentFile.parentFile
        check(NativeArchive.restore(apk, cache, version, abi, key)!!.cacheHit)
        val damaged = File(restored.directory, "libcompat_entry.so"); damaged.setWritable(true); damaged.writeText("damaged")
        check(!NativeArchive.restore(apk, cache, version, abi, key)!!.cacheHit)
        check(runCatching { NativeArchive.restore(apk, File(work, "wrong-${System.nanoTime()}"), version, abi, "00000000000000000000000000000000") }.isFailure)
    }
    val original = ZipFile(apk).use { z -> z.entries().asSequence().associate { e -> e.name to z.getInputStream(e).use { it.readBytes() } } }
    val asset = original.keys.first { it.startsWith(NativeArchive.PREFIX) }
    for (mode in 0..2) {
        val damaged = File(work, "bad-$mode.apk")
        val bytes = original.getValue(asset).copyOf()
        if (mode == 0) bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        if (mode == 1) bytes[20] = 127
        write(damaged, original + (asset to if (mode == 2) bytes.copyOf(bytes.size - 1) else bytes))
        check(runCatching { IntegrityVerifier.verify(damaged, IntegritySeal.publicHex(signing)) }.isFailure)
        val abi = asset.removePrefix(NativeArchive.PREFIX).substringBefore('/')
        check(runCatching { NativeArchive.restore(damaged, File(work, "bad-cache-$mode-${System.nanoTime()}"), version, abi, key) }.isFailure)
    }
    val excluded = File(work, "excluded.apk"); write(excluded, entries)
    check(NativeProtection.protect(excluded, true, key, markers, setOf("libcompat_entry.so")).encryptedFiles.size == 4)
    val invalid = File(work, "invalid.apk"); write(invalid, entries + ("lib/x86/libinvalid.so" to byteArrayOf(1)))
    check(NativeProtection.protect(invalid, true, key, markers).encryptedFiles.none { it.startsWith("lib/x86/") })
    val disabled = File(work, "disabled.apk"); write(disabled, entries)
    check(NativeProtection.protect(disabled, false, key, markers).encryptedFiles.isEmpty())
    val budget = File(work, "budget.apk")
    val small = entries.getValue("lib/x86/libcompat_standalone.so")
    write(budget, entries + (1..5).associate { "lib/x86/libbudget$it.so" to small.copyOf(4 * 1024 * 1024) } +
        ("lib/x86/libhuge.so" to small.copyOf(4 * 1024 * 1024 + 1)))
    val limited = NativeProtection.protect(budget, true, key, markers)
    check(limited.retainedFiles.getValue("lib/x86/libhuge.so").contains("per-file"))
    check(limited.retainedFiles.values.any { it.contains("per-ABI") })
    val alias = File(work, "soname.apk")
    write(alias, entries - "lib/x86/libcompat_dependency.so" +
        ("lib/x86/libalias.so" to entries.getValue("lib/x86/libcompat_dependency.so")))
    check(NativeProtection.protect(alias, true, key, markers).retainedFiles.getValue("lib/x86/libalias.so").contains("DT_NEEDED"))
    println("NATIVE_PROTECTION_PASSED: four ABI ELF dependencies, encryption/exclusions/fail-closed selection, GCM/tamper/truncation, cache recovery and integrity")
}
