package com.wpf.utils.jiagu

import com.wpf.utils.ResourceManager
import com.wpf.utils.tools.ZipalignUtil
import java.io.File
import java.nio.file.Files

/** Signs APKs even when already signed; XAPK members must share one certificate. */
object PackageSigner {
    fun sign(signFile: String, signAlias: String, keyStorePassword: String, keyPassword: String,
             outApkPath: String = "", inputApkPath: String, reserveInput: Boolean = true,
             androidSdkPath: String = ""): String {
        val input = File(inputApkPath).absoluteFile
        require(XapkArchive.isPackage(input)) { "请选择可读取的 APK 或 XAPK：$input" }
        require(File(signFile).isFile) { "签名文件不存在" }
        val output = if (outApkPath.isBlank()) File(input.parentFile,
            input.nameWithoutExtension + "_sign." + input.extension.lowercase()) else File(outApkPath).absoluteFile
        require(input.canonicalFile != output.canonicalFile) { "签名输出不能覆盖源文件" }
        if (input.extension.equals("xapk", true)) {
            XapkArchive.transform(input, output) { members, _ ->
                val certificates = members.map { member ->
                    val signed = Files.createTempFile(member.file.parentFile.toPath(), ".signed-", ".apk").toFile()
                    signApk(member.file, signed, signFile, signAlias, keyStorePassword, keyPassword, androidSdkPath)
                    val certificate = certificate(signed)
                    XapkArchive.publish(signed, member.file)
                    certificate
                }
                check(certificates.distinct().size == 1) { "XAPK 内 APK 签名证书不一致" }
            }
        } else signApk(input, output, signFile, signAlias, keyStorePassword, keyPassword, androidSdkPath)
        if (!reserveInput) check(input.delete()) { "签名成功，但无法删除源文件：$input" }
        println("签名完成：$output")
        return output.path
    }

    private fun signApk(input: File, output: File, store: String, alias: String, storePass: String, keyPass: String, sdk: String) {
        val work = Files.createTempDirectory(output.parentFile.toPath(), ".apk-sign-").toFile()
        try {
            val aligned = File(work, "aligned.apk")
            val sdkRoot = sdk.ifBlank { System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: run {
                val properties = java.util.Properties()
                File("local.properties").takeIf { it.isFile }?.inputStream()?.use { properties.load(it) }
                properties.getProperty("sdk.dir", "")
            } }
            val zipalign = sdkRoot.takeIf { it.isNotBlank() }?.let { File(it, "build-tools").listFiles() }
                ?.sortedByDescending { dir -> dir.name.split('.').joinToString(".") { it.padStart(6, '0') } }
                ?.map { File(it, if (File.separatorChar == '\\') "zipalign.exe" else "zipalign") }
                ?.firstOrNull { it.isFile }
            if (zipalign != null) run(listOf(zipalign.path, "-P", "16", "-f", "4", input.path, aligned.path))
            else check(ZipalignUtil.zipalign(input.path, aligned.path, false) && aligned.isFile) { "APK 对齐失败" }
            val staged = File(work, "signed.apk")
            run(signer() + listOf("sign", "--ks", File(store).absolutePath, "--ks-key-alias", alias,
                "--ks-pass", "env:XAPK_STORE_PASS", "--key-pass", "env:XAPK_KEY_PASS",
                "--out", staged.path, aligned.path),
                mapOf("XAPK_STORE_PASS" to storePass, "XAPK_KEY_PASS" to keyPass))
            certificate(staged)
            XapkArchive.publish(staged, output)
        } finally { work.deleteRecursively() }
    }

    internal fun certificate(apk: File): String {
        val result = run(signer() + listOf("verify", "--print-certs", apk.path))
        return Regex("Signer #\\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)").findAll(result)
            .map { it.groupValues[1].lowercase() }.toList().sorted().joinToString(",").also {
                check(it.isNotEmpty()) { "无法读取 APK 签名证书" }
            }
    }

    private fun signer() = listOf(File(System.getProperty("java.home"), "bin/java" +
        if (File.separatorChar == '\\') ".exe" else "").path, "-jar", ResourceManager.getResourceFile("apksigner.jar").path)

    private fun run(command: List<String>, environment: Map<String, String> = emptyMap()): String {
        val process = ProcessBuilder(command).redirectErrorStream(true).apply { environment().putAll(environment) }.start()
        val message = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { "APK 签名/校验工具失败：${environment.values.fold(message) { text, secret -> if (secret.isEmpty()) text else text.replace(secret, "***") }.take(3000)}" }
        return message
    }
}
