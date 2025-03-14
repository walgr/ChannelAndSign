package com.wpf.tools.fileupload

import com.wpf.utils.http.HttpClient
import net.dongliu.apk.parser.ApkParsers
import java.io.File

object ApkUpload {
    val filterExtension = arrayOf("apk")
    const val RETRY_TIME = 3
    private var curRetryTime = 0

    fun upload(serviceBaseUrl: String, apkFile: File) {
        val apkMetaData = ApkParsers.getMetaInfo(apkFile)
        val packageName = apkMetaData.packageName
        val versionName = apkMetaData.versionName
        println("上传文件到:${serviceBaseUrl}/uploadHap， 文件路径:${apkFile.path}")
        HttpClient.uploadFile(
            "${serviceBaseUrl}/uploadApk",
            apkFile,
            packageName = packageName,
            versionName = versionName,
            null,
            false
        ) {
            if (it) {
                println("上传成功")
            } else {
                println("上传失败")
                if (curRetryTime++ < RETRY_TIME) {
                    println("第${curRetryTime + 1}次重试")
                    upload(serviceBaseUrl, apkFile)
                }
            }
        }
    }
}