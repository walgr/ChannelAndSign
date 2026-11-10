package com.wpf.utils.jiagu

import java.io.Closeable
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Resource APKs are read by native mmap; stored entries must retain their data alignment. */
internal class ResourceZip(file: File) : Closeable {
    private val counter = object : FilterOutputStream(file.outputStream().buffered()) {
        var count = 0L
        override fun write(value: Int) { out.write(value); count++ }
        override fun write(data: ByteArray, offset: Int, length: Int) { out.write(data, offset, length); count += length }
    }
    private val zip = ZipOutputStream(counter)
    fun add(source: ZipEntry, input: InputStream, stored: Boolean = false) {
        val target = ZipEntry(source.name)
        if (source.time >= 315532800000L && source.time <= 4354819199000L) target.time = source.time
        if (stored || source.method == ZipEntry.STORED || source.name == "resources.arsc") {
            target.method = ZipEntry.STORED; target.size = source.size; target.crc = source.crc
            val alignment = if (source.name.startsWith("lib/") && source.name.endsWith(".so")) 16384 else 4
            val padding = ((alignment - (counter.count + 30 + target.name.toByteArray(Charsets.UTF_8).size + 4) % alignment) % alignment).toInt()
            target.extra = ByteArray(4 + padding).apply {
                this[0] = 0xfe.toByte(); this[1] = 0xca.toByte(); this[2] = padding.toByte(); this[3] = (padding ushr 8).toByte()
            }
        }
        zip.putNextEntry(target); input.copyTo(zip); zip.closeEntry()
    }
    override fun close() = zip.close()
}
