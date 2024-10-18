package com.wpf.utils.http

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.createCheck
import io.ktor.client.request.*
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object CacheFile {

    /**
     * @param outFilePath 下载的文件目录带文件名
     */
    fun downloadFile(
        serverUrl: String,
        request: HttpRequestBuilder.() -> Unit = {},
        outFilePath: String,
        callback: ((File?) -> Unit)? = null
    ) {
        val cacheFile = File(outFilePath)
        if (cacheFile.exists() && cacheFile.length() != 0L) {
            callback?.invoke(cacheFile)
            return
        }
        cacheFile.createCheck(true)
        println("开始下载工具:${ResourceManager.serverBaseUrl}getResources?name=${cacheFile.name}")
        HttpClient.downloadFile(serverUrl, request, cacheFile, 0, callback)
    }

    suspend fun downloadFileSuspend(
        serverUrl: String,
        request: HttpRequestBuilder.() -> Unit = {},
        outFilePath: String,
    ): File? {
        return suspendCoroutine { continuation ->
            downloadFile(serverUrl, request, outFilePath) {
                continuation.resume(it)
            }
        }
    }
}