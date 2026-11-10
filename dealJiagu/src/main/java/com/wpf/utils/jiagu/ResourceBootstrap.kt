package com.wpf.utils.jiagu

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/** Conservative closure of every Manifest resource reference, across all configurations. */
internal object ResourceBootstrap {
    data class Kept(val files: Set<String>, val reason: String)
    private class Binary(val bytes: ByteArray) {
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun range(at: Int, length: Int) { require(at >= 0 && length >= 0 && at.toLong() + length <= bytes.size) { "资源依赖数据越界" } }
        fun u8(at: Int): Int { range(at, 1); return bytes[at].toInt() and 255 }
        fun u16(at: Int): Int { range(at, 2); return b.getShort(at).toInt() and 65535 }
        fun i32(at: Int): Int { range(at, 4); return b.getInt(at) }
        fun chunks(start: Int, end: Int, visit: (Int, Int, Int, Int) -> Unit) {
            range(start, end - start); var at = start
            while (at < end) {
                range(at, 8); val header = u16(at + 2); val size = i32(at + 4)
                require(header >= 8 && size >= header && size <= end - at) { "资源依赖 chunk 无效" }
                visit(at, u16(at), header, size); at += size
            }
        }
        fun refs(at: Int, found: MutableSet<Int>, files: MutableSet<String>? = null, strings: List<String> = emptyList()) {
            range(at, 8); require(u16(at) >= 8)
            val value = i32(at + 4)
            when (u8(at + 3)) {
                1, 2, 7, 8 -> if (value != 0) found.add(value)
                3 -> if (files != null) {
                    require(value in strings.indices)
                    strings[value].takeIf { it.startsWith("res/") }?.let { files.add(it) }
                }
            }
        }
        fun strings(pool: Int, size: Int): List<String> {
            val header = u16(pool + 2); val count = i32(pool + 8); val start = i32(pool + 20)
            require(header >= 28 && count >= 0 && header.toLong() + count.toLong() * 4 <= start && start <= size)
            val utf8 = i32(pool + 16) and 0x100 != 0
            return (0 until count).map { index ->
                val offset = i32(pool + header + index * 4)
                require(offset >= 0 && offset < size - start)
                var at = pool + start + offset
                fun length(): Int {
                    if (utf8) { val v = u8(at++); return if (v and 128 == 0) v else ((v and 127) shl 8) or u8(at++) }
                    val v = u16(at); at += 2
                    return if (v and 32768 == 0) v else (((v and 32767) shl 16) or u16(at)).also { at += 2 }
                }
                val chars = length()
                val len = if (utf8) length() else { require(chars in 0..Int.MAX_VALUE / 2); chars * 2 }
                require(len >= 0 && at.toLong() + len + (if (utf8) 1 else 2) <= pool.toLong() + size)
                String(bytes, at, len, if (utf8) Charsets.UTF_8 else Charsets.UTF_16LE)
            }
        }
    }
    private data class Node(val refs: MutableSet<Int> = linkedSetOf(), val files: MutableSet<String> = linkedSetOf())

    fun keep(zip: ZipFile): Kept {
        val all = zip.entries().asSequence().filter { !it.isDirectory && it.name.startsWith("res/") }.map { it.name }.toSet()
        if (all.isEmpty()) return Kept(emptySet(), "no res files")
        val tableEntry = zip.getEntry("resources.arsc") ?: return Kept(all, "no resource table; retain all res files")
        val data = Binary(zip.getInputStream(tableEntry).use { it.readBytes() })
        require(data.u16(0) == 2 && data.i32(4) == data.bytes.size)
        var strings: List<String> = emptyList()
        val nodes = linkedMapOf<Int, Node>()
        val packages = mutableSetOf<Int>()
        data.chunks(data.u16(2), data.bytes.size) { chunk, type, header, size ->
            if (type == 1) strings = data.strings(chunk, size)
            if (type == 0x200) {
                require(header >= 284)
                val packageId = data.i32(chunk + 8); packages.add(packageId)
                val typeOffset = if (header >= 288) data.i32(chunk + 284) else 0
                data.chunks(chunk + header, chunk + size) { t, kind, h, s ->
                    if (kind == 0x201) {
                        require(h >= 20)
                        val typeId = data.u8(t + 8) + typeOffset
                        val flags = data.u8(t + 9); val count = data.i32(t + 12); val start = data.i32(t + 16)
                        val width = if (flags and 2 != 0) 2 else 4
                        require(count >= 0 && h.toLong() + count.toLong() * width <= start && start <= s)
                        for (i in 0 until count) {
                            val index: Int; val offset: Int
                            if (flags and 1 != 0) { index = data.u16(t + h + i * 4); offset = data.u16(t + h + i * 4 + 2) * 4 }
                            else if (flags and 2 != 0) { index = i; offset = data.u16(t + h + i * 2).let { if (it == 65535) -1 else it * 4 } }
                            else { index = i; offset = data.i32(t + h + i * 4) }
                            if (offset == -1) continue
                            require(offset >= 0 && offset <= s - start - 8 && index <= 65535 && typeId in 1..255)
                            val entry = t + start + offset
                            val entrySize = data.u16(entry); val entryFlags = data.u16(entry + 2)
                            require(entryFlags and 8 == 0) { "资源内容加密暂不支持 compact 资源项，请关闭资源压缩格式后重试" }
                            require(entrySize >= 8 && entrySize <= t + s - entry)
                            val node = nodes.getOrPut((packageId shl 24) or (typeId shl 16) or index) { Node() }
                            if (entryFlags and 1 != 0) {
                                require(entrySize >= 16)
                                val parent = data.i32(entry + 8); if (parent != 0) node.refs.add(parent)
                                val maps = data.i32(entry + 12)
                                require(maps >= 0 && entrySize.toLong() + maps.toLong() * 12 <= t.toLong() + s - entry)
                                for (map in 0 until maps) {
                                    val at = entry + entrySize + map * 12
                                    node.refs.add(data.i32(at)); data.refs(at + 4, node.refs, node.files, strings)
                                }
                            } else { require(entrySize <= t + s - entry - 8); data.refs(entry + entrySize, node.refs, node.files, strings) }
                        }
                    }
                }
            }
        }
        val pending = java.util.ArrayDeque<Int>()
        fun xml(bytes: ByteArray) {
            val b = Binary(bytes)
            require(b.u16(0) == 3 && b.i32(4) == bytes.size) { "启动资源 XML 不是标准二进制格式" }
            val found = linkedSetOf<Int>()
            b.chunks(b.u16(2), bytes.size) { at, kind, header, size ->
                if (kind == 0x102) {
                    val ext = at + header; val start = b.u16(ext + 8); val width = b.u16(ext + 10); val count = b.u16(ext + 12)
                    require(width >= 20 && start >= 20 && header.toLong() + start + count.toLong() * width <= size)
                    for (i in 0 until count) b.refs(ext + start + i * width + 12, found)
                }
            }
            pending.addAll(found)
        }
        xml(zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readBytes() })
        val seen = mutableSetOf<Int>(); val kept = linkedSetOf<String>()
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst(); if (!seen.add(id)) continue
            val node = nodes[id]
            if (node == null) {
                if ((id ushr 24) in packages) return Kept(all, "unresolved local/split resource reference; retain all res files")
                continue
            }
            pending.addAll(node.refs)
            for (file in node.files) {
                if (file !in all) return Kept(all, "external/split resource file; retain all res files")
                if (kept.add(file) && file.endsWith(".xml")) xml(zip.getInputStream(zip.getEntry(file)).use { it.readBytes() })
            }
        }
        return Kept(kept, "Manifest references and transitive table/XML dependencies across all configurations")
    }
}
