package com.wpf.utils.jiagu

import java.io.File
import java.io.FilterOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Changes only global resource-table file paths. Resource IDs and key/type pools stay intact. */
object ResourceProtection {
    data class Result(val integrityFiles: Int, val integrityBytes: Long, val renamedFiles: Int,
                      val pathObfuscation: Boolean)

    fun protect(apk: File, obfuscatePaths: Boolean): Result {
        ZipFile(apk).use { zip ->
            val entries = zip.entries().asSequence().toList()
            require(entries.map { it.name }.distinct().size == entries.size) { "APK 包含重复 ZIP 路径" }
            require(entries.none { it.name == "assets/jiagu-integrity.bin" || it.name == "assets/jiagu-integrity.sig" }) {
                "APK 使用了加固保留的完整性记录路径"
            }
            val resources = entries.filter { !it.isDirectory && isResource(it.name) }
            val table = zip.getEntry("resources.arsc")
            val arsc = if (obfuscatePaths && table != null) zip.getInputStream(table).use { it.readBytes() } else null
            val names = if (arsc != null) rewriteTable(arsc, entries.filter { !it.isDirectory }.map { it.name }.toSet()) else emptyMap()
            if (names.isNotEmpty()) rewriteApk(apk, zip, names, requireNotNull(arsc))
            return Result(resources.size, resources.sumOf { it.size }, names.size, obfuscatePaths)
        }
    }

    internal fun isResource(name: String) = name == "resources.arsc" || name.startsWith("res/") ||
        (name.startsWith("assets/") && name != "assets/jiagu-integrity.bin" && name != "assets/jiagu-integrity.sig")

    internal fun rewriteTable(bytes: ByteArray, existing: Set<String>): Map<String, String> {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u16(at: Int): Int { require(at >= 0 && at <= bytes.size - 2); return b.getShort(at).toInt() and 65535 }
        fun i32(at: Int): Int { require(at >= 0 && at <= bytes.size - 4); return b.getInt(at) }
        require(bytes.size >= 12 && u16(0) == 2 && i32(4) == bytes.size) { "资源混淆需要标准二进制 resources.arsc" }
        var chunk = u16(2)
        require(chunk >= 12 && chunk <= bytes.size)
        var pool = -1
        while (chunk < bytes.size) {
            require(chunk <= bytes.size - 8)
            val size = i32(chunk + 4); val header = u16(chunk + 2)
            require(header >= 8 && size >= header && size <= bytes.size - chunk) { "资源表 chunk 越界" }
            if (u16(chunk) == 1) { require(pool == -1) { "资源表包含多个全局字符串池" }; pool = chunk }
            chunk += size
        }
        if (pool == -1) return emptyMap()
        val header = u16(pool + 2); val size = i32(pool + 4)
        require(header >= 28)
        val count = i32(pool + 8); val styles = i32(pool + 12)
        val start = i32(pool + 20); val styleStart = i32(pool + 24)
        require(count >= 0 && styles >= 0 && header.toLong() + (count.toLong() + styles) * 4 <= start && start <= size)
        val end = if (styleStart == 0) pool + size else {
            require(styleStart in start..size); pool + styleStart
        }
        val utf8 = i32(pool + 16) and 0x100 != 0
        val replacements = linkedMapOf<String, String>()
        val occupied = existing.toMutableSet()
        val random = SecureRandom()
        for (i in 0 until count) {
            val offset = i32(pool + header + i * 4)
            require(offset >= 0 && offset.toLong() < end.toLong() - pool - start)
            var at = pool + start + offset
            fun length(): Int {
                if (utf8) {
                    require(at < end); val a = bytes[at++].toInt() and 255
                    if (a and 128 == 0) return a
                    require(at < end); return ((a and 127) shl 8) or (bytes[at++].toInt() and 255)
                }
                require(at <= end - 2); val a = u16(at); at += 2
                if (a and 32768 == 0) return a
                require(at <= end - 2); val low = u16(at); at += 2
                return ((a and 32767) shl 16) or low
            }
            val charLength = length()
            val byteLength = if (utf8) length() else {
                require(charLength <= Int.MAX_VALUE / 2); charLength * 2
            }
            val terminator = if (utf8) 1 else 2
            require(byteLength >= 0 && at.toLong() + byteLength + terminator <= end &&
                bytes[at + byteLength].toInt() == 0 && (utf8 || bytes[at + byteLength + 1].toInt() == 0))
            val charset = if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE
            val original = String(bytes, at, byteLength, charset)
            if (original !in existing || !original.startsWith("res/") || !original.all { it.code in 32..126 }) continue
            val slash = original.lastIndexOf('/')
            val filename = original.substring(slash + 1)
            val stemLength = filename.indexOf('.').let { if (it < 0) filename.length else it }
            if (stemLength == 0) continue
            val replacement = replacements[original] ?: run {
                var candidate: String? = null
                repeat(128) {
                    if (candidate == null) {
                        val name = original.substring(0, slash + 1) +
                            (0 until stemLength).map { ('a'.code + random.nextInt(26)).toChar() }.joinToString("") +
                            filename.substring(stemLength)
                        if (occupied.add(name)) candidate = name
                    }
                }
                candidate?.also { replacements[original] = it }
            } ?: continue
            val encoded = replacement.toByteArray(charset)
            require(encoded.size == byteLength && replacement.length == charLength)
            encoded.copyInto(bytes, at)
        }
        // Renamed strings are no longer lexically sorted. Offsets, styles and all IDs remain unchanged.
        if (replacements.isNotEmpty()) b.putInt(pool + 16, i32(pool + 16) and 1.inv())
        return replacements
    }

    private fun rewriteApk(apk: File, zip: ZipFile, names: Map<String, String>, arsc: ByteArray) {
        val staged = Files.createTempFile(apk.absoluteFile.parentFile.toPath(), ".resource-", ".apk").toFile()
        try {
            val counter = object : FilterOutputStream(staged.outputStream()) {
                var count = 0L
                override fun write(value: Int) { out.write(value); count++ }
                override fun write(data: ByteArray, offset: Int, length: Int) { out.write(data, offset, length); count += length }
            }
            ZipOutputStream(counter).use { out ->
                zip.entries().asSequence().forEach { source ->
                    val target = ZipEntry(names[source.name] ?: source.name)
                    target.time = source.time
                    val table = source.name == "resources.arsc"
                    if (table || source.method == ZipEntry.STORED) {
                        target.method = ZipEntry.STORED
                        target.size = if (table) arsc.size.toLong() else source.size
                        target.crc = if (table) java.util.zip.CRC32().apply { update(arsc) }.value else source.crc
                        val alignment = if (source.name.startsWith("lib/") && source.name.endsWith(".so")) 16384 else 4
                        val padding = ((alignment - (counter.count + 30 + target.name.toByteArray(Charsets.UTF_8).size + 4) % alignment) % alignment).toInt()
                        target.extra = ByteArray(4 + padding).apply {
                            this[0] = 0x35; this[1] = 0xd9.toByte()
                            this[2] = padding.toByte(); this[3] = (padding ushr 8).toByte()
                        }
                    }
                    out.putNextEntry(target)
                    if (table) out.write(arsc) else zip.getInputStream(source).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
            // Close the source handle before replacing the file on Windows.
            zip.close()
            XapkArchive.publish(staged, apk)
        } finally { staged.delete() }
    }
}
