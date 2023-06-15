package com.wpf.base.dealfile.util

import com.wpf.base.dealfile.apksignerPath
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


/**
 * Android apk签名
 */
object ApkSignerUtil {
    init {
        apksignerPath = File("").canonicalPath + File.separator + "apksigner.jar"
        if (!File(apksignerPath).exists()) {
            javaClass.getResource("/apksigner.jar")?.openStream()?.copyTo(FileOutputStream(apksignerPath))
        }
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
        inputApkPath: String
    ) {
        val cmd = arrayOf(
            "java",
            "-jar",
            apksignerPath,
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
        val result = Runtime.getRuntime().exec(cmd)
        val resultStr = result.errorStream.readBytes().decodeToString()
        println("$inputApkPath 签名结果:" + if (resultStr.isEmpty()) "成功" else "失败")
//        ApkSignerTool.main(
//            arrayOf(
//                "sign",
//                "--ks",
//                signFile,
//                "--ks-key-alias",
//                signAlias,
//                "--ks-pass",
//                "pass:$keyStorePassword",
//                "--key-pass",
//                "pass:$keyPassword",
//                "--out",
//                outSignPath,
//                inputApkPath
//            )
//        )
    }
}