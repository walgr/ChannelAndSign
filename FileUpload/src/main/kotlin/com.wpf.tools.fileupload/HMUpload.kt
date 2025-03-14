package com.wpf.tools.fileupload

import com.wpf.utils.http.HttpClient
import com.wpf.utils.tools.HMUnpackingUtil
import java.io.File

object HMUpload {

    val filterExtension = arrayOf("hap", "app", "hsp")

    const val RETRY_TIME = 3
    private var curRetryTime = 0
    fun upload(serviceBaseUrl: String, hapFile: File) {
        val hapInfo = HMUnpackingUtil.getHapInfo(hapFile)
        if (hapInfo != null && !hapInfo.bundleName.isNullOrEmpty()) {
            println("上传文件到:${serviceBaseUrl}/uploadHap， 文件路径:${hapFile.path}")
            HttpClient.uploadFile(
                serverUrl = "${serviceBaseUrl}/uploadHap",
                uploadFile = hapFile,
                packageName = hapInfo.bundleName ?: "",
                versionName = hapInfo.versionName ?: ""
            ) {
                if (it) {
                    println("上传成功")
                } else {
                    println("上传失败")
                    if (curRetryTime++ < RETRY_TIME) {
                        println("第${curRetryTime + 1}次重试")
                        upload(serviceBaseUrl, hapFile)
                    }
                }
            }
        }
    }
}