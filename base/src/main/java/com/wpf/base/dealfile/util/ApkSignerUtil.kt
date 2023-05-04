package com.wpf.base.dealfile.util

import com.android.apksigner.ApkSignerTool

/**
 * Android apk签名
 */
object ApkSignerUtil {

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
        inputApkPath: String
    ) {
        ApkSignerTool.main(
            arrayOf(
                "sign",
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
        )
    }
}