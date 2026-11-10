package com.wpf.utils.jiagu

import com.wpf.util.jiagulibrary.utils.NativeArchive
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.*

/** Fail closed on dependency analysis: an unreadable ELF keeps its entire ABI in plaintext. */
object NativeProtection {
    data class Elf(val needed: Set<String>, val soname: String?)
    data class Result(val enabled: Boolean, val integrityFiles: Int, val encryptedFiles: List<String>,
        val encryptedBytes: Long, val retainedFiles: Map<String, String>,
        val maxFileBytes: Long = 4L * 1024 * 1024, val maxAbiBytes: Long = 16L * 1024 * 1024,
        val method: String = "AES-128-GCM / 64 KiB chunks", val runtimePlaintext: String = "private no-backup native cache",
        val splitScope: String = "base APK only; split native libraries remain unchanged")
    fun isLibrary(name: String) = name.matches(Regex("lib/[^/]+/[^/]+\\.so"))
    fun key(secret: String): String = MessageDigest.getInstance("SHA-256")
        .digest(("jiagu-native-v1\u0000" + secret).toByteArray(Charsets.UTF_8)).take(16)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    fun elf(bytes: ByteArray, abi: String): Elf {
        val machine = mapOf("x86" to (1 to 3), "x86_64" to (2 to 62), "armeabi-v7a" to (1 to 40), "arm64-v8a" to (2 to 183))[abi]
            ?: error("unsupported ABI")
        require(bytes.size >= 64 && bytes[0] == 127.toByte() && bytes.copyOfRange(1, 4).contentEquals("ELF".toByteArray()) &&
            bytes[4].toInt() == machine.first && bytes[5].toInt() == 1) { "invalid ELF header" }
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u16(p: Int) = b.getShort(p).toInt() and 65535
        fun word(p: Int, wide: Boolean): Long = if (wide) b.getLong(p).also { require(it >= 0) } else b.getInt(p).toLong() and 0xffffffffL
        fun offset(p: Long, size: Long = 1): Int { require(p >= 0 && size >= 0 && p <= bytes.size.toLong() - size); return p.toInt() }
        require(u16(16) == 3 && u16(18) == machine.second) { "ELF is not matching ET_DYN" }
        val wide = machine.first == 2; val ph = word(if (wide) 32 else 28, wide)
        val stride = u16(if (wide) 54 else 42); val count = u16(if (wide) 56 else 44)
        require(stride >= (if (wide) 56 else 32) && count in 1..1024)
        data class Segment(val type: Int, val file: Long, val address: Long, val size: Long)
        val segments = (0 until count).map { i ->
            val p = offset(ph + i.toLong() * stride, stride.toLong())
            Segment(b.getInt(p), word(p + if (wide) 8 else 4, wide), word(p + if (wide) 16 else 8, wide), word(p + if (wide) 32 else 16, wide))
        }
        val dynamic = segments.single { it.type == 2 }; offset(dynamic.file, dynamic.size)
        val step = if (wide) 16 else 8
        require(dynamic.size % step == 0L && dynamic.size / step <= 65536)
        val tags = mutableListOf<Pair<Long, Long>>(); var ended = false
        for (i in 0 until (dynamic.size / step).toInt()) {
            val p = offset(dynamic.file + i * step); val tag = word(p, wide)
            if (tag == 0L) { ended = true; break }; tags += tag to word(p + step / 2, wide)
        }
        require(ended)
        val address = tags.single { it.first == 5L }.second
        val size = tags.single { it.first == 10L }.second
        val load = segments.single { it.type == 1 && address >= it.address && address - it.address < it.size }
        require(size <= load.size - (address - load.address))
        val start = offset(load.file + address - load.address, size)
        fun string(index: Long): String {
            require(index < size); val p = offset(start + index); var end = p
            while (end < start + size && bytes[end] != 0.toByte()) end++
            require(end < start + size && end - p in 1..1024)
            return String(bytes, p, end - p, Charsets.UTF_8)
        }
        return Elf(tags.filter { it.first == 1L }.map { string(it.second) }.toSet(),
            tags.singleOrNull { it.first == 14L }?.let { string(it.second) })
    }

    fun protect(apk: File, encrypt: Boolean, key: String, markers: Map<String, File>, excluded: Set<String> = emptySet()): Result {
        val work = Files.createTempDirectory(apk.absoluteFile.parentFile.toPath(), ".native-encrypt-").toFile()
        try {
            ZipFile(apk).use { zip ->
                require(zip.entries().asSequence().none { it.name.startsWith(NativeArchive.PREFIX) || it.name.endsWith("/libjiagu_abi_marker.so") }) { "APK 使用了 SO 加固保留路径" }
                val libraries = zip.entries().asSequence().filter { !it.isDirectory && isLibrary(it.name) }.toList()
                val retained = linkedMapOf<String, String>(); val selected = mutableListOf<ZipEntry>()
                if (!encrypt) return Result(false, libraries.size, emptyList(), 0, libraries.associate { it.name to "content encryption disabled" })
                libraries.groupBy { it.name.split('/')[1] }.forEach { (abi, entries) ->
                    val parsed = entries.associate { entry -> entry.name to runCatching {
                        require(entry.size in 64..(128L * 1024 * 1024))
                        zip.getInputStream(entry).use { elf(it.readBytes(), abi) }
                    }.getOrNull() }
                    val incoming = parsed.values.filterNotNull().flatMap { it.needed }.toSet(); var used = 0L
                    entries.sortedWith(compareBy<ZipEntry> { it.size }.thenBy { it.name }).forEach { e ->
                        val name = e.name.substringAfterLast('/'); val info = parsed[e.name]
                        val reason = when {
                            !encrypt -> "content encryption disabled"
                            parsed.values.any { it == null } -> "unreadable ELF or unsupported ABI; entire ABI retained"
                            e.name in excluded || name in excluded || info?.soname in excluded -> "configured or split dependency exclusion"
                            name in incoming || info?.soname in incoming -> "DT_NEEDED dependency; system linker retained"
                            !name.matches(Regex("lib[A-Za-z0-9_.+-]+\\.so")) -> "unsupported library filename"
                            e.size > 4L * 1024 * 1024 -> "per-file 4 MiB speed budget"
                            e.size > 16L * 1024 * 1024 - used -> "per-ABI 16 MiB speed budget"
                            else -> null
                        }
                        if (reason != null) retained[e.name] = reason else { selected += e; used += e.size }
                    }
                }
                if (selected.isEmpty()) return Result(encrypt, libraries.size, emptyList(), 0, retained)
                val names = selected.map { it.name }.toSet(); val output = File(work, "protected.apk")
                ResourceZip(output).use { out ->
                    zip.entries().asSequence().filter { it.name !in names }.forEach { entry -> zip.getInputStream(entry).use { out.add(entry, it, stored = isLibrary(entry.name)) } }
                    selected.forEachIndexed { i, entry ->
                        val plain = File(work, "$i.so"); val payload = File(work, "$i.bin")
                        zip.getInputStream(entry).use { input -> plain.outputStream().use { input.copyTo(it) } }
                        NativeArchive.encrypt(plain, payload, key)
                        val asset = NativeArchive.PREFIX + entry.name.removePrefix("lib/") + ".bin"
                        payload.inputStream().use { out.add(ZipEntry(asset).apply { size = payload.length(); crc = CRC32().apply { update(payload.readBytes()) }.value }, it, true) }
                    }
                    selected.map { it.name.split('/')[1] }.distinct().forEach { abi ->
                        val marker = requireNotNull(markers[abi]) { "Missing ABI marker: $abi" }; check(marker.isFile)
                        marker.inputStream().use { out.add(ZipEntry("lib/$abi/libjiagu_abi_marker.so").apply {
                            size = marker.length(); crc = CRC32().apply { update(marker.readBytes()) }.value
                        }, it, true) }
                    }
                }
                zip.close(); XapkArchive.publish(output, apk)
                return Result(true, libraries.size, names.sorted(), selected.sumOf { it.size }, retained)
            }
        } finally { work.deleteRecursively() }
    }
}
