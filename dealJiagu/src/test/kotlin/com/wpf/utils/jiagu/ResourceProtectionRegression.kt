package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.util.jiagulibrary.utils.IntegrityVerifier
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

fun main(args: Array<String>) {
    val root = File(args[0]); val sdk = File(args[1])
    val work = File(root, "build/resource-regression").apply { mkdirs() }
    fun table(path: String, utf8: Boolean): ByteArray {
        val payload = ByteArrayOutputStream()
        fun length(value: Int) {
            if (utf8) {
                if (value > 127) payload.write((value ushr 8) or 128)
                payload.write(value and 255)
            } else { payload.write(value and 255); payload.write(value ushr 8) }
        }
        val encoded = path.toByteArray(if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
        length(path.length); if (utf8) length(encoded.size)
        payload.write(encoded); payload.write(0); if (!utf8) payload.write(0)
        while (payload.size() % 4 != 0) payload.write(0)
        val data = ByteArray(12 + 32 + payload.size())
        val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        b.putShort(2); b.putShort(12); b.putInt(data.size); b.putInt(0)
        b.putShort(1); b.putShort(28); b.putInt(data.size - 12)
        b.putInt(1); b.putInt(0); b.putInt(if (utf8) 0x101 else 1); b.putInt(32); b.putInt(0); b.putInt(0)
        b.put(payload.toByteArray()); return data
    }
    for (utf8 in listOf(true, false)) {
        for (path in listOf("res/drawable/probe.9.png", "res/raw/" + "a".repeat(140) + ".txt")) {
            val data = table(path, utf8); val before = data.copyOf()
            val renamed = ResourceProtection.rewriteTable(data, setOf(path))
            val replacement = renamed.getValue(path)
            check(replacement.length == path.length && replacement != path)
            check(replacement.substringBeforeLast('/') == path.substringBeforeLast('/'))
            check(replacement.substringAfter('.') == path.substringAfter('.'))
            check(data.size == before.size && ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(28) and 1 == 0)
            check(before.copyOfRange(0, 28).contentEquals(data.copyOfRange(0, 28)))
        }
        val path = "res/raw/a.txt"
        val occupied = ('a'..'z').map { "res/raw/$it.txt" }.toSet()
        check(ResourceProtection.rewriteTable(table(path, utf8), occupied).isEmpty())
        check(ResourceProtection.rewriteTable(table("assets/probe.txt", utf8), setOf("assets/probe.txt")).isEmpty())
        val malformed = table(path, utf8).apply {
            ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(40, Int.MAX_VALUE)
        }
        check(runCatching { ResourceProtection.rewriteTable(malformed, setOf(path)) }.isFailure)
    }
    val manifest = File(work, "AndroidManifest.xml").apply { writeText("""
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.wpf.jiagu.resources">
          <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="33"/>
          <application android:label="@string/resource_probe"/>
        </manifest>
    """.trimIndent()) }
    val original = File(work, "original.apk")
    compileResourceFixture(work, sdk, manifest, original)
    val protected = File(work, "protected.apk"); original.copyTo(protected, true)
    val originalEntries = ZipFile(original).use { zip -> zip.entries().asSequence().associate { entry ->
        entry.name to zip.getInputStream(entry).use { it.readBytes() }
    } }
    val baseline = File(work, "integrity-only.apk"); original.copyTo(baseline, true)
    check(ResourceProtection.protect(baseline, false).renamedFiles == 0)
    check(baseline.readBytes().contentEquals(original.readBytes()))
    val result = ResourceProtection.protect(protected, true)
    check(result.renamedFiles == 8 && result.integrityFiles == 11)
    ZipFile(protected).use { zip ->
        check(zip.getEntry("resources.arsc").method == java.util.zip.ZipEntry.STORED)
        check(zip.getInputStream(zip.getEntry("AndroidManifest.xml")).readBytes().contentEquals(originalEntries.getValue("AndroidManifest.xml")))
        check(zip.getInputStream(zip.getEntry("assets/resource_probe.txt")).readBytes().contentEquals(originalEntries.getValue("assets/resource_probe.txt")))
        for ((path, content) in originalEntries.filterKeys { it.startsWith("res/") }) {
            check(zip.getEntry(path) == null)
            check(zip.entries().asSequence().any { it.name.startsWith(path.substringBeforeLast('/') + "/") &&
                zip.getInputStream(it).use { stream -> stream.readBytes() }.contentEquals(content) })
        }
    }
    fun tool(exe: String, vararg params: String): String {
        val p = ProcessBuilder(listOf(File(sdk, "build-tools/35.0.0/$exe.exe").path) + params).redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText(); check(p.waitFor() == 0) { text }; return text
    }
    val ids = Regex("resource 0x[0-9a-fA-F]+ [^\\r\\n]+")
    check(ids.findAll(tool("aapt2", "dump", "resources", original.path)).map { it.value }.toList() ==
        ids.findAll(tool("aapt2", "dump", "resources", protected.path)).map { it.value }.toList())
    tool("zipalign", "-c", "4", protected.path)
    ZipArchive(protected.toPath()).use {
        it.add(BytesSource(byteArrayOf(1), "classes.dex", 0))
        it.add(BytesSource(byteArrayOf(2), "assets/libjiagu_x86_64.so", 0))
    }
    val key = IntegritySeal.key(File(work, "key"))
    IntegritySeal.seal(protected, key)
    IntegrityVerifier.verify(protected, IntegritySeal.publicHex(key))
    println("RESOURCE_PROTECTION_PASSED: UTF8/UTF16, collision/bounds, resource IDs/names, file bytes, assets paths, stored ARSC/alignment, content signature")
}
