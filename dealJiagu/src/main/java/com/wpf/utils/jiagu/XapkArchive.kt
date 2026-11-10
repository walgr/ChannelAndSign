package com.wpf.utils.jiagu

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.wpf.utils.jiagu.utils.ApplicationHelper
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/** A transaction over an XAPK. Only original archive members are repacked. */
object XapkArchive {
    data class Member(val path: String, val file: File, val split: String, val packageName: String, val version: String)

    fun isPackage(file: File) = file.isFile && file.extension.lowercase(Locale.ROOT) in setOf("apk", "xapk")

    fun transform(input: File, output: File, action: (List<Member>, Member) -> Unit): File {
        require(input.isFile) { "XAPK 不存在：$input" }
        require(input.canonicalFile != output.canonicalFile) { "输出不能覆盖源 XAPK" }
        val work = Files.createTempDirectory(output.absoluteFile.parentFile.toPath(), ".xapk-").toFile()
        try {
            val root = File(work, "contents").apply { mkdirs() }
            val names = linkedSetOf<String>()
            val folded = hashSetOf<String>()
            ZipFile(input).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val name = entry.name
                    require(name.isNotBlank() && !name.contains('\\') && !name.contains(':') &&
                        !name.startsWith('/') && name.split('/').none { it == ".." || it == "." }) {
                        "XAPK 包含非法路径：$name"
                    }
                    require(folded.add(name.trimEnd('/').lowercase(Locale.ROOT))) { "XAPK 包含重复路径：$name" }
                    val target = File(root, name)
                    require(target.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())) { "非法 XAPK 路径" }
                    names.add(name)
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile.mkdirs()
                        zip.getInputStream(entry).use { source -> target.outputStream().use { source.copyTo(it) } }
                    }
                }
            }
            val members = names.filter { it.endsWith(".apk", true) }.map { name ->
                val apk = File(root, name)
                val factory = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                }
                val manifest = factory.newDocumentBuilder().parse(
                    ApplicationHelper.manifestXml(apk).byteInputStream(Charsets.UTF_8)).documentElement
                Member(name, apk, manifest.getAttribute("split"), manifest.getAttribute("package"),
                    manifest.getAttributeNS("http://schemas.android.com/apk/res/android", "versionCode"))
            }
            require(members.isNotEmpty()) { "XAPK 中没有 APK" }
            val base = members.singleOrNull { it.split.isEmpty() } ?: error("XAPK 必须包含且仅包含一个主 APK")
            require(base.packageName.isNotBlank() && members.all {
                it.packageName == base.packageName && it.version == base.version
            }) { "XAPK 内 APK 的包名或版本不一致" }
            require(members.map { it.split }.distinct().size == members.size) { "XAPK 包含重复 split 标识" }
            val manifestFile = File(root, "manifest.json")
            val metadata = manifestFile.takeIf { it.isFile }?.let { JsonParser.parseString(it.readText()) }
            if (metadata?.isJsonObject == true) {
                metadata.asJsonObject.get("package_name")?.let {
                    require(it.asString == base.packageName) { "manifest.json 与主 APK 包名不一致" }
                }
            }
            action(members, base)
            metadata?.let {
                refreshMetadata(it, root, members)
                if (it.isJsonObject && it.asJsonObject.has("total_size")) {
                    it.asJsonObject.addProperty("total_size", names.filter { n -> n != "manifest.json" }
                        .sumOf { n -> File(root, n).takeIf { f -> f.isFile }?.length() ?: 0L })
                }
                manifestFile.writeText(it.toString(), Charsets.UTF_8)
            }
            val staged = File(work, "result.xapk")
            ZipOutputStream(staged.outputStream().buffered()).use { zip ->
                names.forEach { name ->
                    zip.putNextEntry(ZipEntry(name))
                    val file = File(root, name)
                    if (file.isFile) file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            publish(staged, output)
            return output
        } finally { work.deleteRecursively() }
    }

    private fun refreshMetadata(value: JsonElement, root: File, members: List<Member>) {
        if (value.isJsonArray) value.asJsonArray.forEach { refreshMetadata(it, root, members) }
        if (!value.isJsonObject) return
        val obj = value.asJsonObject
        val path = listOf("file", "file_name", "name").mapNotNull { key ->
            obj.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
        }.firstOrNull { name -> members.any { it.path == name } }
        if (path != null) {
            val file = File(root, path)
            listOf("size", "file_size").filter { obj.has(it) }.forEach { obj.addProperty(it, file.length()) }
            listOf("md5", "sha1", "sha256").filter { obj.has(it) }.forEach { key ->
                val digest = MessageDigest.getInstance(mapOf("md5" to "MD5", "sha1" to "SHA-1", "sha256" to "SHA-256").getValue(key))
                file.inputStream().use { stream ->
                    val bytes = ByteArray(65536)
                    while (true) { val count = stream.read(bytes); if (count < 0) break; digest.update(bytes, 0, count) }
                }
                obj.addProperty(key, digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) })
            }
        }
        obj.entrySet().toList().forEach { refreshMetadata(it.value, root, members) }
    }

    internal fun publish(source: File, output: File) {
        try { Files.move(source.toPath(), output.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
