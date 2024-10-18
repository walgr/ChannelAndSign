package com.wpf.utils.http

import com.wpf.utils.isWinRuntime
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object HttpClient {
    val client: HttpClient by lazy {
        HttpClient(CIO) {
            engine {
                requestTimeout = 120000
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 120000
            }
        }
    }

    fun downloadFile(
        serverUrl: String,
        request: HttpRequestBuilder.() -> Unit = {},
        outFile: File,
        length: Long = 0L,
        callback: ((File?) -> Unit)? = null
    ) {
        runBlocking {
            val lastByte = length - 1

            var start = outFile.length()
            val output = FileOutputStream(outFile, true)
            val chunkSize = if (length >= 1024 * 1024 * 10) (length / if (isWinRuntime) length / 10 else 100) else length
            do {
                val end = min(start + chunkSize - 1, lastByte)
                val data = client.get(serverUrl) {
                    timeout {
                        requestTimeoutMillis = 300000
                    }
                    if (length != 0L) {
                        header("Range", "bytes=${start}-${end}")
                    }
                    request.invoke(this)
                }.body<ByteArray>()
                output.write(data)
                start += chunkSize
                if (length != 0L) {
                    println("正在下载文件:${outFile.path},进度:${start * 100 / length}%")
                }
            } while (end < lastByte && length != 0L)
            callback?.invoke(outFile)
        }
    }

    fun uploadFile(
        serverUrl: String,
        uploadFile: File,
        packageName: String,
        versionName: String,
        callback: ((Boolean) -> Unit)?
    ) {
        if (!uploadFile.exists()) {
            println("请上传文件")
            callback?.invoke(false)
        }
        runBlocking {
            runCatching {
                val responseData = client.put(serverUrl) {
                    timeout {
                        requestTimeoutMillis = 300000
                    }
                    setBody(MultiPartFormDataContent(
                        formData {
                            append("package", packageName)
                            append("appVersion", versionName)
                            append("file", uploadFile.readBytes(), apkHeader(uploadFile.path))
                        }
                    ))
                    var lastProcess = 0L
                    onUpload { bytesSentTotal, contentLength ->
                        val curProcess = bytesSentTotal * 100 / contentLength
                        if (curProcess != lastProcess) {
                            lastProcess = curProcess
                            println("文件:${uploadFile.name} 大小:${uploadFile.length()} 上传进度:${curProcess}%")
                        }
                    }
                }
                if (responseData.status == HttpStatusCode.OK) {
                    callback?.invoke(true)
                } else {
                    callback?.invoke(false)
                }
            }.onFailure {
                callback?.invoke(false)
            }
        }
    }

    private fun apkHeader(filePath: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
            append(HttpHeaders.ContentDisposition, "filename=\"${File(filePath).name}\"")
        }
    }
}