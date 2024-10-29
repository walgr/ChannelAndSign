package com.wpf.utils.pgyupload.pgy

import com.google.gson.Gson
import com.wpf.utils.pgyupload.pgy.data.ApkInfo
import com.wpf.utils.pgyupload.pgy.data.BaseResponse
import com.wpf.utils.pgyupload.pgy.data.CheckResponseInfo
import com.wpf.utils.pgyupload.pgy.data.TokenResponse
import com.wpf.utils.pgyupload.pgy.http.HttpHelper
import com.wpf.utils.pgyupload.pgy.http.HttpHelper.apkHeader
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.File

object Upload {
    private const val baseUrl = "https://www.pgyer.com/apiv2/app/"
    private const val getToken = "getCOSToken"
    private const val buildInfo = "buildInfo"
    private const val check = "check"

    private var tokenResponse: TokenResponse? = null
    private fun getToken(apiKey: String, buildType: String, callback: (TokenResponse) -> Unit) {
        if (tokenResponse != null) {
            callback.invoke(tokenResponse!!)
            return
        }
        HttpHelper.post<BaseResponse<TokenResponse>>(baseUrl + getToken, request = {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
            }
            url {
                parameters.append("_api_key", apiKey)
                parameters.append("buildType", buildType)
            }
        }) {
            if (it?.data == null) {
                println("获取Token失败，请重试")
            } else {
                tokenResponse = it.data
                callback.invoke(it.data)
            }
        }
    }

    fun uploadApk(
        apiKey: String,
        buildType: String,
        apk: File,
        uploadFileName: Boolean = true,
        callback: (Boolean?) -> Unit
    ) {
        getToken(apiKey, buildType) { token ->
            println("正在上传文件:${apk.name},大小:${apk.length()}")
            HttpHelper.post<String>(token.endpoint, request = {
                timeout {
                    requestTimeoutMillis = 300000
                }
                setBody(MultiPartFormDataContent(
                    formData {
                        append("\"key\"", token.key)
                        append("\"signature\"", token.params?.signature ?: "")
                        append("\"x-cos-security-token\"", token.params?.`x-cos-security-token` ?: "")
                        if (uploadFileName) {
                            append("\"x-cos-meta-file-name\"", apk.name)
                        }
                        append("\"file\"", apk.readBytes(), apkHeader(apk.name))
                    }
                ))
                var lastProcess = 0L
                onUpload { bytesSentTotal, contentLength ->
                    val curProcess = bytesSentTotal * 100 / contentLength
                    if (curProcess != lastProcess) {
                        lastProcess = curProcess
                        println("上传进度:${curProcess}%")
                    }
                }
            }) {
                callback.invoke(it != null)
            }
        }
    }

    fun getUploadResult(apiKey: String, buildType: String, requestTime: Int = 0, callback: (ApkInfo?) -> Unit) {
        getToken(apiKey, buildType) { token ->
            println("正在获取发布状态......")
            HttpHelper.get<BaseResponse<ApkInfo>>(baseUrl + buildInfo, {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                }
                url {
                    parameters.append("_api_key", apiKey)
                    parameters.append("buildKey", token.key)
                }
            }) {
                if (it?.data == null) {
                    if (requestTime < 20) {
                        runBlocking {
                            Thread.sleep(1000)
                            getUploadResult(apiKey, buildType, requestTime + 1, callback)
                        }
                    } else {
                        callback.invoke(null)
                    }
                } else {
                    callback.invoke(it.data)
                    tokenResponse = null
                }
            }
        }
    }

    /**
     * 检测App是否有更新
     */
    fun checkNewVersion(
        apiKey: String,
        appKey: String,
        buildVersion: String = "",
        buildBuildVersion: String = "",
        callback: (CheckResponseInfo?) -> Unit
    ) {
        HttpHelper.post<BaseResponse<CheckResponseInfo>>(baseUrl + check, {
            url {
                parameters.append("_api_key", apiKey)
                parameters.append("appKey", appKey)
                if (buildVersion.isNotEmpty()) {
                    parameters.append("buildVersion", buildVersion)
                }
                if (buildBuildVersion.isNotEmpty()) {
                    parameters.append("buildBuildVersion", buildBuildVersion)
                }
            }
        }) {
            callback.invoke(it?.data)
        }
    }
}