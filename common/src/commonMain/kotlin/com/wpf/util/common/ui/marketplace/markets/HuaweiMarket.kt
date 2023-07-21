package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.gson.reflect.TypeToken
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.marketplace.markets.base.packageName
import com.wpf.util.common.ui.utils.Callback
import com.wpf.util.common.ui.utils.SuccessCallback
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.settings
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File

/**
 * api地址：https://developer.huawei.com/consumer/cn/doc/development/AppGallery-connect-References/agcapi-obtain_token-0000001158365043
 */
data class HuaweiMarket(
    var clientId: String = "",
    var clientSecret: String = ""
) : Market {

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = MarketType.华为.channelName

    @Transient
    override val baseUrl: String = "https://connect-api.cloud.huawei.com/api"

    @Transient
    private var token = gson.fromJson<MutableMap<Long, String>>(
        settings.getString("huaweiToken", "{}"),
        object : TypeToken<MutableMap<Long, String>>() {}.type
    ) ?: mutableMapOf()

    private fun saveToken() {
        settings.putString("huaweiToken", gson.toJson(token))
    }

    private fun clearToken() {
        settings.putString("huaweiToken", "")
    }

    private fun getEfficientToken(): String {
        var efficientToken = ""
        val noEfficientToken = mutableListOf<Long>()
        token.forEach { (t, u) ->
            if ((System.currentTimeMillis() / 1000) - t < 172800) {
                efficientToken = u
            } else {
                noEfficientToken.add(t)
            }
        }
        noEfficientToken.forEach {
            token.remove(it)
        }
        saveToken()
        return efficientToken
    }

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun query(uploadData: UploadData) {
        super.query(uploadData)
        if (uploadData.packageName().isNullOrEmpty()) return
        getAppInfo(uploadData.packageName()!!, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                println(t)
            }

        })
    }


    private fun getAppInfo(packageName: String, callback: SuccessCallback<String>) {
        getAppId(packageName, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                Http.get("$baseUrl/publish/v2/app-info", request = {
                    timeout {
                        requestTimeoutMillis = 30000
                    }
                    headers {
                        append("client_id", clientId)
                        bearerAuth(getEfficientToken())
                    }
                    url {
                        parameters.append("client_id", clientId)
                        parameters.append("appId", t.getAppId())
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        callback.onSuccess(t)
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }

    override fun push(uploadData: UploadData, callback: Callback<String>) {

    }

    //提交审核
    private fun submit() {

    }

    //在这里填写更新文案
    private fun updateAppLanguageInfo(packageName: String, description: String, callback: SuccessCallback<String>) {
        getAppId(packageName, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                Http.put("$baseUrl/publish/v2/app-language-info", {
                    timeout {
                        requestTimeoutMillis = 30000
                    }
                    headers {
                        append("client_id", clientId)
                        bearerAuth(getEfficientToken())
                    }
                    url {
                        parameters.append("appId", t.getAppId())
                    }
                    setBody(HuaweiUpdateLanguageBody(description))
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiBaseResponse::class.java)
                        if (response.ret?.code == 0) {
                            callback.onSuccess("更新成功")
                        } else {
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }
        })
    }

    private fun updateAppFileInfo(
        packageName: String,
        infoBody: HuaweiUpdateFileInfoBody,
        callback: SuccessCallback<String>
    ) {
        getAppId(packageName, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                Http.put("$baseUrl/publish/v2/app-file-info", {
                    timeout {
                        requestTimeoutMillis = 30000
                    }
                    headers {
                        append("client_id", clientId)
                        bearerAuth(getEfficientToken())
                    }
                    parameters {
                        append("appId", t.getAppId())
                        append("releaseType", "1")
                    }
                    setBody(infoBody)
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiBaseResponse::class.java)
                        if (response.ret?.code == 0) {
                            callback.onSuccess("")
                        } else {
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }
        })
    }

    private fun uploadApk(uploadData: UploadData, callback: Callback<HuaweiUploadFileResponse>) {
        getAppId(uploadData.packageName()!!, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                getUploadUrl(t.getAppId(), "apk", callback = object : SuccessCallback<HuaweiUploadUrlResponse> {
                    override fun onSuccess(t: HuaweiUploadUrlResponse) {
                        Http.put(t.uploadUrl, {
                            timeout {
                                requestTimeoutMillis = 120000
                            }
                            val uploadApk = uploadData.apk.abiApk.find { uploadAbi().contains(it.abi) }
                            setBody(MultiPartFormDataContent(formData {
                                append("authCode", t.authCode)
                                append("fileCount", 1)
                                append("file", File(uploadApk!!.filePath).readBytes(), apkHeader(uploadApk.fileName))
                            }))
                        }, object : Callback<String> {
                            override fun onSuccess(t: String) {
                                val response = gson.fromJson(t, HuaweiUploadFileResponse::class.java)
                                if (response.result?.UploadFileRsp?.ifSuccess == 1) {
                                    val uploadApk = uploadData.apk.abiApk.find { uploadAbi().contains(it.abi) }
                                    updateAppFileInfo(uploadData.packageName()!!, HuaweiUpdateFileInfoBody(
                                        5, listOf(
                                            HuaweiFileInfoBody(
                                                uploadApk?.name ?: "",
                                                response.result.UploadFileRsp.fileInfoList?.getOrNull(0)?.fileDestUlr
                                                    ?: ""
                                            )
                                        )
                                    ), object : SuccessCallback<String> {
                                        override fun onSuccess(t: String) {
                                            callback.onSuccess(response)
                                        }

                                        override fun onFail(msg: String) {
                                            callback.onFail(msg)
                                        }

                                    })
                                } else {
                                    callback.onFail("")
                                }

                            }

                            override fun onFail(msg: String) {
                                callback.onFail(msg)
                            }

                        })
                    }

                })
            }
        })
    }

    private fun uploadScreenShotList(uploadData: UploadData, callback: SuccessCallback<String>) {
        if (uploadData.imageList.isNullOrEmpty()) {
            callback.onSuccess("没有需要上传的截图")
            return
        }
        val uploadScreenShotResult = mutableMapOf<String, HuaweiUploadFileResponse>()
        uploadData.imageList.forEach {
            uploadScreenShot(uploadData.packageName()!!, it, object : SuccessCallback<HuaweiUploadFileResponse> {
                override fun onSuccess(t: HuaweiUploadFileResponse) {
                    uploadScreenShotResult[it] = t
                    if (uploadScreenShotResult.size == uploadData.imageList.size) {
                        updateAppFileInfo(uploadData.packageName()!!, HuaweiUpdateFileInfoBody(
                            2, uploadScreenShotResult.map { map ->
                                HuaweiFileInfoBody(
                                    map.key,
                                    map.value.result?.UploadFileRsp?.fileInfoList?.getOrNull(0)?.fileDestUlr ?: ""
                                )
                            }
                        ), object : SuccessCallback<String> {
                            override fun onSuccess(t: String) {
                                callback.onSuccess("上传截图组成功")
                            }

                            override fun onFail(msg: String) {
                                callback.onFail(msg)
                            }

                        })
                    } else {
                        println("正在上传截图，目前已成功${uploadScreenShotResult.size}个")
                    }
                }

                override fun onFail(msg: String) {
                    super.onFail(msg)
                    callback.onFail(msg)
                }
            })
        }
    }

    private fun uploadScreenShot(
        packageName: String,
        screenShot: String,
        callback: SuccessCallback<HuaweiUploadFileResponse>
    ) {
        getAppId(packageName, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                getUploadUrl(
                    t.getAppId(),
                    File(screenShot).extension,
                    callback = object : SuccessCallback<HuaweiUploadUrlResponse> {
                        override fun onSuccess(t: HuaweiUploadUrlResponse) {
                            Http.put(t.uploadUrl, {
                                timeout {
                                    requestTimeoutMillis = 120000
                                }
                                setBody(MultiPartFormDataContent(formData {
                                    append("authCode", t.authCode)
                                    append("fileCount", 1)
                                    append("parseType", 1)
                                    append("file", File(screenShot).readBytes(), apkHeader(screenShot))
                                }))
                            }, object : Callback<String> {
                                override fun onSuccess(t: String) {
                                    val response = gson.fromJson(t, HuaweiUploadFileResponse::class.java)
                                    if (response.result?.UploadFileRsp?.ifSuccess == 1) {
                                        callback.onSuccess(response)
                                    } else {
                                        callback.onFail("")
                                    }
                                }

                                override fun onFail(msg: String) {
                                    callback.onFail(msg)
                                }

                            })
                        }

                    })
            }
        })
    }

    private fun getUploadUrl(
        appId: String,
        suffix: String = "apk",
        callback: SuccessCallback<HuaweiUploadUrlResponse>
    ) {
        getToken(object : Callback<String> {
            override fun onSuccess(t: String) {
                Http.get("$baseUrl/publish/v2/upload-url", {
                    timeout {
                        requestTimeoutMillis = 30000
                    }
                    headers {
                        append("client_id", clientId)
                        bearerAuth(getEfficientToken())
                    }
                    url {
                        parameters.append("appId", appId)
                        parameters.append("suffix", suffix)
                        parameters.append("releaseType", "1")
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiUploadUrlResponse::class.java)
                        if (response?.ret?.code == 0) {
                            callback.onSuccess(response)
                        } else {
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }

            override fun onFail(msg: String) {

            }
        })
    }

    @Transient
    private val appIdMap = mutableMapOf<String, HuaweiAppIdResponse>()
    private fun getAppId(packageName: String, callback: SuccessCallback<HuaweiAppIdResponse>) {
        val appId = appIdMap[packageName]
        if (appId != null) {
            callback.onSuccess(appId)
            return
        }
        getToken(object : Callback<String> {
            override fun onSuccess(t: String) {
                Http.get("$baseUrl/publish/v2/appid-list", request = {
                    timeout {
                        requestTimeoutMillis = 30000
                    }
                    headers {
                        append("client_id", clientId)
                        bearerAuth(getEfficientToken())
                    }
                    url {
                        parameters.append("packageName", packageName)
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiAppIdResponse::class.java)
                        if (response.ret?.code == 0) {
                            appIdMap[packageName] = response
                            callback.onSuccess(response)
                        } else {
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }

            override fun onFail(msg: String) {

            }

        })
    }

    private fun getToken(callback: Callback<String>) {
        val efficientToken = getEfficientToken()
        if (efficientToken.isNotEmpty()) {
            callback.onSuccess(efficientToken)
            return
        }
        Http.post("$baseUrl/oauth2/v1/token", request = {
            timeout {
                requestTimeoutMillis = 30000
            }
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8))
            }
            setBody(
                HuaweiTokenRequest(clientId, clientSecret)
            )
        }, object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, HuaweiTokenResponse::class.java)
                if (response?.access_token?.isNotEmpty() == true) {
                    token[System.currentTimeMillis() / 1000] = response.access_token
                    saveToken()
                    callback.onSuccess(response.access_token)
                } else {
                    callback.onFail("")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }

        })
    }

    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is HuaweiMarket) return
        val clientId = remember { mutableStateOf(market.clientId) }
        clientId.value = market.clientId
        val clientSecret = remember { mutableStateOf(market.clientSecret) }
        clientSecret.value = market.clientSecret
        Column {
            InputView(input = clientId, hint = "请配置ClientId") {
                clientId.value = it
                market.clientId = it
            }
            InputView(input = clientSecret, hint = "请配置ClientSecret") {
                clientSecret.value = it
                market.clientSecret = it
            }
        }
    }

    override fun clearInitData() {
        super.clearInitData()
        clearToken()
    }
}

@Serializable
internal data class HuaweiTokenRequest(
    val client_id: String,
    val client_secret: String,
    val grant_type: String = "client_credentials",
)

@Serializable
internal data class HuaweiTokenResponse(
    val access_token: String? = null,
    val expires_in: String? = null,
)

@Serializable
internal data class HuaweiAppIdResponse(
    val appids: List<HuaweiAppId>? = null
) : HuaweiBaseResponse() {

    fun getAppId(): String {
        return appids?.getOrNull(0)?.value ?: ""
    }
}

@Serializable
internal data class HuaweiAppId(
    val key: String? = null,
    val value: String? = null,
) : HuaweiBaseResponse()

@Serializable
internal data class HuaweiUpdateLanguageBody(
    val lang: String = "zh-CN",       //必填
    val newFeatures: String? = null,
)

@Serializable
internal data class HuaweiUpdateFileInfoBody(
    val fileType: Int,                          //2：应用介绍截图。5：软件包，如RPK、APK、AAB等。
    val files: List<HuaweiFileInfoBody>? = null,
)

@Serializable
internal data class HuaweiFileInfoBody(
    val fileName: String,           //文件名称，包括文件的后缀名。 fileType为5时，此参数必选。
    val fileDestUrl: String,
)

@Serializable
internal data class HuaweiUploadFileResponse(
    val result: UploadFileResult? = null
)

@Serializable
internal data class UploadFileResult(
    val resultCode: String? = null,
    val UploadFileRsp: UploadFileRsp? = null
)

@Serializable
internal data class UploadFileRsp(
    val ifSuccess: Int? = 0,
    val fileInfoList: List<UploadFileInfo>? = null
)

@Serializable
internal data class UploadFileInfo(
    val fileDestUlr: String? = null,
    val size: Long? = null,
    val imageResolution: String? = null,
    val imageResolutionSingature: String? = null,
)

@Serializable
internal data class HuaweiUploadUrlResponse(
    val uploadUrl: String,                      //文件上传的URL，格式为https://{domainname}.com。该地址包含了文件上传授权码，且根据用户站点信息分配不同的地址。
    val chunkUploadUrl: String,                 //分片上传地址，只有apk才会返回。
    val authCode: String,
) : HuaweiBaseResponse()

@Serializable
internal open class HuaweiBaseResponse(
    val ret: BaseResponse? = null
)

@Serializable
internal data class BaseResponse(
    val code: Int = 0,
    val msg: String? = null,
)