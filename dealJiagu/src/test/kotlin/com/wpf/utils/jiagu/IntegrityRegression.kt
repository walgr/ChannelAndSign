package com.wpf.utils.jiagu

import com.wpf.util.jiagulibrary.utils.IntegrityVerifier
import com.wpf.util.jiagulibrary.utils.RuntimeCompatibility
import java.io.File
import java.util.zip.*

fun main(args: Array<String>) {
    val work = File(args[0], "build/integrity-regression").apply { mkdirs() }
    val key = IntegritySeal.key(File(work, "key"))
    val publicKey = IntegritySeal.publicHex(key)
    check(publicKey == IntegritySeal.publicHex(IntegritySeal.key(File(work, "key"))))
    val entries = mapOf("AndroidManifest.xml" to "manifest".toByteArray(), "classes.dex" to "encrypted dex".toByteArray(),
        "assets/libjiagu_x86_64.so" to "shell ELF".toByteArray(),
        "resources.arsc" to "resource table".toByteArray(), "res/raw/probe.txt" to "raw".toByteArray(),
        "assets/probe.txt" to "asset".toByteArray(), "lib/x86/libbusiness.so" to "business ELF".toByteArray())
    fun write(file: File, contents: Map<String, ByteArray>) = ZipOutputStream(file.outputStream()).use { out ->
        contents.forEach { (name, bytes) -> out.putNextEntry(ZipEntry(name)); out.write(bytes); out.closeEntry() }
    }
    val apk = File(work, "sealed.apk"); write(apk, entries); IntegritySeal.seal(apk, key)
    IntegrityVerifier.verify(apk, publicKey)
    val extracted = File(work, "lib.so").apply { writeBytes(entries.getValue("assets/libjiagu_x86_64.so")) }
    check(IntegrityVerifier.matchesShell(extracted, "libjiagu_x86_64.so"))
    extracted.appendBytes(byteArrayOf(0))
    check(!IntegrityVerifier.matchesShell(extracted, "libjiagu_x86_64.so"))
    val original = ZipFile(apk).use { zip -> zip.entries().asSequence().associate { entry ->
        entry.name to zip.getInputStream(entry).readBytes()
    } }
    fun reject(contents: Map<String, ByteArray>, trustedKey: String = publicKey) {
        val bad = File(work, "tampered.apk"); write(bad, contents)
        check(runCatching { IntegrityVerifier.verify(bad, trustedKey) }.isFailure)
    }
    for (name in entries.keys + listOf("assets/jiagu-integrity.bin", "assets/jiagu-integrity.sig")) {
        reject(original + (name to original.getValue(name).copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }))
        reject(original - name)
    }
    val attackerKey = IntegritySeal.key(File(work, "attacker-key"))
    val forged = File(work, "forged.apk"); write(forged, entries + ("classes.dex" to "replaced dex".toByteArray()))
    IntegritySeal.seal(forged, attackerKey)
    check(runCatching { IntegrityVerifier.verify(forged, publicKey) }.isFailure)
    reject(original, IntegritySeal.publicHex(attackerKey))
    reject(original + ("assets/libjiagu_x86.so" to byteArrayOf(1)))
    reject(original + ("res/raw/added.txt" to byteArrayOf(1)))
    reject(original + ("assets/added.txt" to byteArrayOf(1)))
    reject(original + ("lib/x86/libadded.so" to byteArrayOf(1)))
    val large = File(work, "many-resources.apk")
    write(large, entries + (0 until 600).associate { "assets/large-resource-$it.txt" to byteArrayOf(it.toByte()) })
    IntegritySeal.seal(large, key)
    IntegrityVerifier.verify(large, publicKey)
    (21..37).forEach { sdk ->
        listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").forEach { RuntimeCompatibility.requireSupported(sdk, it) }
    }
    check(runCatching { RuntimeCompatibility.requireSupported(20, "x86_64") }.isFailure)
    check(runCatching { RuntimeCompatibility.requireSupported(36, "mips") }.isFailure)
    println("SHELL_INTEGRITY_PASSED: signed records, wrong key/re-sign/tamper/deletion rejection, cached SO mismatch, runtime compatibility contract")
}
