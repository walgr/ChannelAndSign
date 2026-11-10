package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.util.jiagulibrary.utils.IntegrityVerifier
import com.wpf.util.jiagulibrary.utils.ResourceArchive
import java.io.File
import java.security.MessageDigest
import java.util.zip.*

fun main(args: Array<String>) {
    val root = File(args[0]); val sdk = File(args[1])
    val work = File(root, "build/resource-encryption-regression").apply { mkdirs() }
    val manifest = File(work, "AndroidManifest.xml").apply { writeText("""
        <manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.wpf.jiagu.encrypted">
        <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="33"/>
        <application android:icon="@drawable/bootstrap_icon" android:theme="@style/BootstrapTheme" android:label="@string/resource_probe"/>
        </manifest>
    """.trimIndent()) }
    val apk = File(work, "encrypted.apk")
    compileResourceFixture(work, sdk, manifest, apk)
    val large = ByteArray(210000).apply { java.util.Random(42).nextBytes(this) }
    ZipArchive(apk.toPath()).use {
        it.add(BytesSource(large, "assets/large.bin", 0))
        it.add(BytesSource(ByteArray(300000), "assets/skipped-large.bin", 0))
        it.add(BytesSource(ByteArray(32), "assets/skipped-video.mp4", 0))
        it.add(BytesSource(byteArrayOf(1), "classes.dex", 0))
        it.add(BytesSource(byteArrayOf(2), "assets/libjiagu_x86_64.so", 0))
    }
    val originals = ZipFile(apk).use { zip -> zip.entries().asSequence().filter { !it.isDirectory }.associate { entry ->
        entry.name to zip.getInputStream(entry).use { it.readBytes() }
    } }
    val secret = ResourceEncryption.key("0123456789abcdef")
    val budgetApk = File(work, "budget.apk"); apk.copyTo(budgetApk, true)
    val budget = ResourceEncryption.encrypt(budgetApk, secret, maxFileBytes = 128, maxTotalBytes = 64)
    check(budget.encryptedBytes <= 64 && budget.skippedFiles.values.contains("total encryption budget"))
    val result = ResourceEncryption.encrypt(apk, secret)
    check(result.skippedFiles["assets/skipped-large.bin"] == "per-file size limit")
    check(result.skippedFiles["assets/skipped-video.mp4"] == "large-media/archive policy")
    check(result.encryptedFiles.containsAll(listOf("res/layout/resource_probe.xml", "res/drawable/business_image.png",
        "res/raw/resource_probe.txt", "assets/resource_probe.txt", "assets/large.bin"))) { result.toString() }
    check(result.bootstrapFiles.toSet() == setOf("res/drawable/bootstrap_bg.xml", "res/drawable-night-v8/bootstrap_bg.xml",
        "res/drawable/bootstrap_image.png", "res/drawable/bootstrap_icon.png")) { result.toString() }
    val signing = IntegritySeal.key(File(work, "key")); IntegritySeal.seal(apk, signing)
    IntegrityVerifier.verify(apk, IntegritySeal.publicHex(signing))
    val version = requireNotNull(IntegrityVerifier.resourceVersion())
    ZipFile(apk).use { zip ->
        result.encryptedFiles.forEach { check(zip.getEntry(it) == null) }
        (result.bootstrapFiles + "resources.arsc" + "AndroidManifest.xml" + "assets/libjiagu_x86_64.so").forEach {
            check(zip.getInputStream(zip.getEntry(it)).readBytes().contentEquals(originals.getValue(it)))
        }
        result.skippedFiles.keys.forEach { check(zip.getInputStream(zip.getEntry(it)).readBytes().contentEquals(originals.getValue(it))) }
    }
    val cache = File(work, "cache-$version")
    val first = ResourceArchive.restore(apk, cache, version, secret)
    check(!first.cacheHit)
    ZipFile(first.file).use { zip ->
        check(zip.getEntry("assets/skipped-large.bin") == null && zip.getEntry("assets/resource_large.bin") == null)
        for (path in result.encryptedFiles + result.bootstrapFiles + "resources.arsc") {
            val entry = zip.getEntry(path)
            check(entry.method == ZipEntry.STORED)
            check(zip.getInputStream(entry).readBytes().contentEquals(originals.getValue(path)))
        }
    }
    check(ResourceArchive.restore(apk, cache, version, secret).cacheHit)
    check(first.file.setWritable(true, true)); first.file.writeBytes(byteArrayOf(0))
    check(!ResourceArchive.restore(apk, cache, version, secret).cacheHit)
    check(runCatching { ResourceArchive.restore(apk, File(work, "wrong-key-$version"), version, "0".repeat(32)) }.isFailure)
    val payload = ZipFile(apk).use { zip -> zip.getInputStream(zip.getEntry(ResourceArchive.ASSET)).readBytes() }
    for (position in listOf(0, 4, 16, 24, 56, 56 + 65552, payload.lastIndex)) {
        val bad = File(work, "tampered-$position.apk")
        ZipOutputStream(bad.outputStream()).use { out ->
            out.putNextEntry(ZipEntry(ResourceArchive.ASSET))
            out.write(payload.copyOf().apply { this[position] = (this[position].toInt() xor 1).toByte() }); out.closeEntry()
        }
        check(runCatching { ResourceArchive.restore(bad, File(work, "bad-$version-$position"), version, secret) }.isFailure)
        check(File(work, "bad-$version-$position").listFiles().orEmpty().none { it.extension == "tmp" || it.extension == "apk" })
    }
    // Final APK alignment is performed by PackageSigner; the runtime-generated cache has no signer.
    for (file in listOf(first.file)) {
        val p = ProcessBuilder(File(sdk, "build-tools/35.0.0/zipalign.exe").path, "-c", "-v", "4", file.path).redirectErrorStream(true).start()
        val text = p.inputStream.bufferedReader().readText(); check(p.waitFor() == 0) { text }
    }
    println("RESOURCE_ENCRYPTION_PASSED: layout/PNG/raw/assets ciphertext-only APK, transitive theme/icon/config retention, chunk authentication, wrong-key/tamper rejection, cache validation/recovery, openFd alignment")
}
