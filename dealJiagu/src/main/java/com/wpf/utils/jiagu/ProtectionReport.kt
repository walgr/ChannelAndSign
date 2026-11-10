package com.wpf.utils.jiagu

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/** Local build evidence only. Never includes keys/passwords or invents device results. */
class ProtectionReport(val packageName: String, val selectionMode: String) {
    val schemaVersion = 1
    val supportedRuntime = "API 21+ armeabi-v7a/arm64-v8a/x86/x86_64; requires supported ART DefineClass/OpenMemory symbols"
    val deviceValidation = "not_run"
    val startupPerformance = "not_measured; collect NDK_JIAGU_PERF on a device"
    var archiveType = "APK"
    var sourceName = ""
    var outputName = ""
    var inputBytes = 0L
    var outputBytes = 0L
    var sizeDeltaBytes = 0L
    var outputSha256 = ""
    var packagingMs = 0L
    var signed = false
    var shellCacheHit = false
    var d2cMethods: List<String> = emptyList()
    var vmpMethods: List<String> = emptyList()
    var skippedNativeMethods: Map<String, String> = emptyMap()
    val extractedCodeItems = mutableListOf<Extracted>()
    val phasesMs = linkedMapOf<String, Long>()
    var baseProtection: JsonObject? = null
    var protectedArchiveMember: String? = null
    var preservedSplits: List<String> = emptyList()
    val coverageNotes = listOf("skippedNativeMethods may still use third-generation extraction",
        "extractedCodeItems counts unique code items, not aliases sharing the same code item",
        "feature split code, ordinary resources and third-party SO are not converted/protected by this stage")

    data class Extracted(val dex: String, val method: String, val bytes: Int)

    fun phase(name: String, since: Long) { phasesMs[name] = (System.nanoTime() - since) / 1_000_000 }

    fun write(input: File, output: File, startedAt: Long) {
        sourceName = input.name; outputName = output.name
        inputBytes = input.length(); outputBytes = output.length(); sizeDeltaBytes = outputBytes - inputBytes
        val hash = MessageDigest.getInstance("SHA-256")
        output.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) { val count = stream.read(buffer); if (count < 0) break; hash.update(buffer, 0, count) }
        }
        outputSha256 = hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        packagingMs = (System.nanoTime() - startedAt) / 1_000_000
        val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
        publish(File(output.path + ".protection.json"), gson.toJson(this))
        val details = baseProtection
        val d2cCount = details?.getAsJsonArray("d2cMethods")?.size() ?: d2cMethods.size
        val vmpCount = details?.getAsJsonArray("vmpMethods")?.size() ?: vmpMethods.size
        val extractionCount = details?.getAsJsonArray("extractedCodeItems")?.size() ?: extractedCodeItems.size
        val skipped = details?.getAsJsonObject("skippedNativeMethods")?.size() ?: skippedNativeMethods.size
        publish(File(output.path + ".protection.md"), """
            # 加固保护报告

            - 文件：$sourceName → $outputName
            - 主包名：$packageName
            - 选择方式：$selectionMode
            - D2C：$d2cCount；VMP：$vmpCount；抽离的唯一 code_item：$extractionCount
            - 跳过 Native 转换：$skipped（不等于未使用第三代方法抽离）
            - 包体：$inputBytes → $outputBytes 字节；增量：$sizeDeltaBytes 字节
            - 打包耗时：$packagingMs 毫秒；已签名：$signed
            - 输出 SHA-256：$outputSha256
            - 运行条件：$supportedRuntime
            - 设备启动验证：未执行；启动性能：未测量
            - XAPK 保留的拆分包：${preservedSplits.size}（功能 split 代码保持原样）

            此报告证明本次打包行为，不能证明目标设备兼容。完整方法清单、跳过原因及分阶段耗时见同名 JSON；设备启动日志标签为 NDK_JIAGU_PERF。
        """.trimIndent() + "\n")
        println("保护报告：${output.path}.protection.json")
    }

    private fun publish(output: File, contents: String) {
        val staged = Files.createTempFile(output.absoluteFile.parentFile.toPath(), ".protection-", ".tmp").toFile()
        try { staged.writeText(contents, Charsets.UTF_8); XapkArchive.publish(staged, output) }
        finally { staged.delete() }
    }

    companion object {
        fun read(output: File): JsonObject = JsonParser.parseString(File(output.path + ".protection.json").readText(Charsets.UTF_8)).asJsonObject
    }
}
