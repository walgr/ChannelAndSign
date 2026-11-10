package com.wpf.utils.jiagu

import com.wpf.util.jiagulibrary.utils.ResourceArchive
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.*

object ResourceEncryption {
    data class Result(val enabled: Boolean, val encryptedFiles: List<String>, val encryptedBytes: Long,
                      val bootstrapFiles: List<String>, val bootstrapReason: String,
                      val skippedFiles: Map<String, String>, val maxFileBytes: Long, val maxTotalBytes: Long,
                      val method: String = "AES-128-GCM", val runtimePlaintext: String = "private no-backup resource APK cache",
                      val resourceTableEncrypted: Boolean = false)
    fun key(secret: String): String = MessageDigest.getInstance("SHA-256")
        .digest(("jiagu-resource-v1\u0000" + secret).toByteArray(Charsets.UTF_8)).take(16)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun encrypt(apk: File, key: String, maxFileBytes: Long = 256 * 1024, maxTotalBytes: Long = 8 * 1024 * 1024): Result {
        require(maxFileBytes > 0 && maxTotalBytes > 0)
        val work = Files.createTempDirectory(apk.absoluteFile.parentFile.toPath(), ".resource-encrypt-").toFile()
        try {
            ZipFile(apk).use { zip ->
                require(zip.getEntry(ResourceArchive.ASSET) == null) { "APK 使用了加固保留的资源加密路径" }
                val kept = ResourceBootstrap.keep(zip)
                val resources = zip.entries().asSequence().filter { !it.isDirectory &&
                    (it.name.startsWith("res/") || (it.name.startsWith("assets/") &&
                        !it.name.matches(Regex("assets/libjiagu_(v8a|v7a|x86|x86_64)\\.so")))) }.toList()
                val skipped = linkedMapOf<String, String>(); var used = 0L
                val encrypted = resources.filter { it.name !in kept.files }.sortedWith(
                    compareBy<ZipEntry> { if (it.name.startsWith("res/layout") || it.name.endsWith(".xml")) 0 else 1 }.thenBy { it.size }.thenBy { it.name }
                ).filter { entry ->
                    val media = entry.name.substringAfterLast('.').lowercase() in setOf("mp4", "mkv", "webm", "mp3", "ogg", "wav", "flac", "zip", "obb")
                    val reason = when { media -> "large-media/archive policy"; entry.size > maxFileBytes -> "per-file size limit";
                        entry.size > maxTotalBytes - used -> "total encryption budget"; else -> null }
                    if (reason != null) skipped[entry.name] = reason else used += entry.size
                    reason == null
                }
                val names = encrypted.map { it.name }.toSet()
                if (names.isEmpty()) return Result(true, emptyList(), 0, kept.files.sorted(), kept.reason, skipped, maxFileBytes, maxTotalBytes)
                val plain = File(work, "resources.apk")
                ZipOutputStream(plain.outputStream().buffered()).use { out ->
                    encrypted.forEach { source ->
                        out.putNextEntry(ZipEntry(source.name))
                        zip.getInputStream(source).use { it.copyTo(out) }; out.closeEntry()
                    }
                }
                val payload = File(work, "resources.bin")
                ResourceArchive.encrypt(plain, apk, payload, key)
                val staged = File(work, "encrypted.apk")
                ResourceZip(staged).use { out ->
                    zip.entries().asSequence().filter { it.name !in names }.forEach { source ->
                        zip.getInputStream(source).use { out.add(source, it) }
                    }
                    val crc = CRC32()
                    payload.inputStream().use { input -> val buffer = ByteArray(65536); while (true) {
                        val n = input.read(buffer); if (n < 0) break; crc.update(buffer, 0, n)
                    } }
                    payload.inputStream().use { out.add(ZipEntry(ResourceArchive.ASSET).apply {
                        size = payload.length(); this.crc = crc.value
                    }, it, stored = true) }
                }
                zip.close()
                XapkArchive.publish(staged, apk)
                return Result(true, names.sorted(), encrypted.sumOf { it.size }, kept.files.sorted(), kept.reason, skipped, maxFileBytes, maxTotalBytes)
            }
        } finally { work.deleteRecursively() }
    }
}
