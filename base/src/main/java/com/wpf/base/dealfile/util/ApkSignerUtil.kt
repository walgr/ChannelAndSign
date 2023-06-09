package com.wpf.base.dealfile.util

import com.wpf.base.dealfile.apksignerPath
import java.io.File
import java.io.FileOutputStream


/**
 * Android apk签名
 */
object ApkSignerUtil {
    init {
        apksignerPath = File("").canonicalPath + File.separator + "apksigner.jar"
        if (!File(apksignerPath).exists()) {
            val openStream = javaClass.getResource("/apksigner.jar")?.openStream()
            val outSteam = FileOutputStream(apksignerPath)
            openStream?.copyTo(outSteam)
            openStream?.close()
            outSteam.close()
        }
    }

    fun dealJar() {
        File(apksignerPath).delete()
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
        return resultStr.isEmpty()
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