package com.wpf.utils.tools

import java.io.File

object SignHelper {

    fun sign(
        signFile: String,
        signAlias: String,
        keyStorePassword: String,
        keyPassword: String,
        outApkPath: String = "",
        inputApkPath: String,
        reserveInput: Boolean = true        //是否保留源文件
    ): String {
        val srcApkFile = File(inputApkPath)
        if (!srcApkFile.exists() || srcApkFile.extension != "apk") {
            println("待签名的apk不存在")
            return ""
        }
        if (ApkSignerUtil.isSign(inputApkPath)) {
            println("$inputApkPath 已签名")
            return ""
        }
        var outSignPathTemp =
            srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension + "_sign." + srcApkFile.extension + ".tmp"
        if (!ZipalignUtil.check(inputApkPath)) {
            val result = ZipalignUtil.zipalign(inputApkPath, outSignPathTemp, false)
            if (!result) {
                File(outSignPathTemp).delete()
                return ""
            }
        } else {
            outSignPathTemp = inputApkPath
        }
        val outSignPath = outApkPath.ifEmpty {
            srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension.replace(
                "_sign",
                ""
            ) + "_sign." + srcApkFile.extension
        }
        ApkSignerUtil.sign(
            signFile,
            signAlias,
            keyStorePassword,
            keyPassword,
            outSignPath = outSignPath,
            inputApkPath = outSignPathTemp,
            false
        )
        if (!reserveInput) {
            srcApkFile.delete()
        }
        if (outSignPathTemp != inputApkPath || !reserveInput) {
            File(outSignPathTemp).delete()
        }
        return outSignPath
    }
}