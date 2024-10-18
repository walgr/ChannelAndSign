package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import java.io.File


/**
 * Android apk签名
 */
object ApkSignerUtil {
    private val apksignerPath: String = ""
        get() {
            return if (field.isEmpty() || !File(field).exists()) {
                ResourceManager.getResourceFile("apksigner.jar").path
            } else field
        }

    fun delJar() {
        ResourceManager.delResourceByPath(apksignerPath)
    }

    fun isSign(apkFilePath: String): Boolean {
        val cmd = arrayOf(
            "verify",
            apkFilePath
        )
        val result = Runtime.getRuntime().exec(RunJar.javaJar(apksignerPath, cmd))
        var resultStr = "DOES NOT VERIFY"
        LogStreamThread(result.inputStream, false, showAllLog = {
            false
        }).start()
        LogStreamThread(result.errorStream, false, showAllLog = {
            resultStr = it
            false
        }).start()
        result.waitFor()
        result.destroy()
        return !resultStr.contains("DOES NOT VERIFY")
    }

    /**
     * apk签名
     * @param signFile 签名文件
     * @param signAlias     别名
     * @param keyStorePassword 别名密码
     * @param keyPassword 密码
     * @param outSignPath 签名输出文件路径
     * @param inputApkPath 待签名文件路径
     */
    fun sign(
        signFile: String,
        signAlias: String,
        keyStorePassword: String,
        keyPassword: String,
        outSignPath: String,
        inputApkPath: String,
        outRenameToInput: Boolean = false,
    ): Boolean {
        val cmd = arrayOf(
            "sign",
            "--v1-signing-enabled",
            "true",
            "--v2-signing-enabled",
            "true",
            "--v3-signing-enabled",
            "false",
            "--ks",
            signFile,
            "--ks-key-alias",
            signAlias,
            "--ks-pass",
            "pass:$keyStorePassword",
            "--key-pass",
            "pass:$keyPassword",
            "--out",
            outSignPath,
            inputApkPath
        )
        val result = Runtime.getRuntime().exec(RunJar.javaJar(apksignerPath, cmd))
        val resultStr = result.errorStream.readBytes().decodeToString()
        if (resultStr.isNotEmpty()) {
            println(resultStr)
        }
        result.waitFor()
        result.destroy()
        if (outRenameToInput) {
            val inputFile = File(inputApkPath)
            inputFile.delete()
            File(outSignPath).renameTo(inputFile)
        }
        return resultStr.isEmpty()
    }
}