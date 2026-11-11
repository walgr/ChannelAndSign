package com.wpf.utils.jiagu

import com.wpf.util.jiagulibrary.utils.DexSource
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** A protected DEX larger than the JVM heap must remain streamable. */
fun main() {
    val folder = Files.createTempDirectory("dex-source-regression-").toFile()
    try {
        val apk = folder.resolve("large.apk")
        val chunk = ByteArray(65536) { (it * 17).toByte() }
        val expected = MessageDigest.getInstance("SHA-256")
        ZipOutputStream(apk.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("classes.dex"))
            repeat(2048) { zip.write(chunk); expected.update(chunk) }
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("assets/jiagu-payload.bin"))
            java.io.DataOutputStream(zip).apply {
                writeInt(0x4a475031); writeInt(112); writeInt(128 * 1024 * 1024)
            }
            zip.closeEntry()
        }
        val actual = MessageDigest.getInstance("SHA-256")
        DexSource(apk.path).use { source ->
            check(source.length == 128L * 1024 * 1024)
            var bytes = 0L
            while (true) {
                val count = source.read(chunk)
                if (count == -1) break
                check(count > 0)
                actual.update(chunk, 0, count)
                bytes += count
            }
            check(bytes == source.length)
        }
        val expectedDigest = expected.digest()
        check(expectedDigest.contentEquals(actual.digest()))
        DexSource(apk.path, expectedDigest).use { source ->
            check(source.startPayload() == source.length - 112 - 4)
            var bytes = 0L
            while (true) {
                val count = source.read(chunk)
                if (count == -1) break
                bytes += count
            }
            check(bytes == source.length - 112)
        }
        val missing = folder.resolve("missing.apk")
        ZipOutputStream(missing.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("classes2.dex")); zip.write(chunk); zip.closeEntry()
        }
        check(runCatching { DexSource(missing.path).close() }.exceptionOrNull() is java.io.IOException)
        for ((label, magic, offset, size) in listOf(
            listOf("magic", 0, 112, 1024),
            listOf("shell", 0x4a475031, 111, 1024),
            listOf("bounds", 0x4a475031, 900, 1024),
            listOf("size", 0x4a475031, 112, 1023)
        )) {
            val invalid = folder.resolve("invalid-$label.apk")
            ZipOutputStream(invalid.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("classes.dex")); zip.write(ByteArray(1024)); zip.closeEntry()
                zip.putNextEntry(ZipEntry("assets/jiagu-payload.bin"))
                java.io.DataOutputStream(zip).apply { writeInt(magic as Int); writeInt(offset as Int); writeInt(size as Int) }
                zip.closeEntry()
            }
            check(runCatching { DexSource(invalid.path).use { it.startPayload() } }.exceptionOrNull() is java.io.IOException)
        }
        println("DexSource: streamed 128 MiB with a 32 MiB heap; digest, payload index, invalid bounds and missing-DEX checks passed")
    } finally { folder.deleteRecursively() }
}
