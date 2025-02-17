package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import com.google.gson.reflect.TypeToken
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.marketplace.markets.base.packageName
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import java.io.File

/**
 * api地址：https://developer.huawei.com/consumer/cn/doc/development/AppGallery-connect-References/agcapi-obtain_token-0000001158365043
 */
data class HuaweiMarket(
    var clientId: String = "", var clientSecret: String = ""
) : Market {

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = MarketType.华为.channelName

    @Transient
    override val baseUrl: String = "https://connect-api.cloud.huawei.com/api"

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

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun query(uploadData: UploadData, callback: Callback<MarketType>) {
        super.query(uploadData, callback)
        if (uploadData.packageName().isNullOrEmpty()) return
        getAppInfo(uploadData.packageName()!!, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.onSuccess(MarketType.华为)
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }


    private fun getAppInfo(packageName: String, callback: SuccessCallback<String>) {
        getAppId(packageName, { callback.onFail(it) }) { appId ->
            Http.get("$baseUrl/publish/v2/app-info", request = {
                headers {
                    append("client_id", clientId)
                    bearerAuth(getEfficientToken(packageName))
                }
                url {
                    parameters.append("client_id", clientId)
                    parameters.append("appId", appId.getAppId())
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
    }

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        updateAppLanguageInfo(uploadData.packageName()!!, uploadData.description, { callback.onFail(it) }) {
            uploadApk(uploadData.apk.abiApk.find { uploadAbi().contains(it.abi) }!!, { callback.onFail(it) }) {
                uploadScreenShotList(uploadData, onFail = { callback.onFail(it) }) {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(10000)
                        submit(uploadData.packageName()!!, { callback.onFail(it) }) {
                            callback.onSuccess(MarketType.华为)
                        }
                    }
                }
            }
        }
    }

    private fun submit(
        packageName: String, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit
    ) {
        submit(packageName, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    //提交审核
    private fun submit(packageName: String, callback: SuccessCallback<String>) {
        getToken(packageName, { callback.onFail(it) }) { token ->
            getAppId(packageName, { callback.onFail(it) }) { appId ->
                Http.post("$baseUrl/publish/v2/app-submit", {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json)
                        append("client_id", clientId)
                        bearerAuth(token)
                    }
                    url {
                        parameters.append("appId", appId.getAppId())
                        parameters.append("releaseType", "1")
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiBaseResponse::class.java)
                        if (response.isSuccess()) {
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
        }
    }

    private fun updateAppLanguageInfo(
        packageName: String, description: String, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit
    ) {
        updateAppLanguageInfo(packageName, description, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val updateLanguageInfoSuccessMap = autoSaveMap("huaweiLanInfo") { mutableMapOf<String, MutableMap<String, String>>() }

    //在这里填写更新文案
    private fun updateAppLanguageInfo(packageName: String, description: String, callback: SuccessCallback<String>) {
        if (updateLanguageInfoSuccessMap[packageName].containsKey(packageName + description)) {
            callback.onSuccess(updateLanguageInfoSuccessMap[packageName][description] ?: "")
            return
        }
        getToken(packageName, { callback.onFail(it) }) { token ->
            getAppId(packageName, { callback.onFail(it) }) { appId ->
                Http.put("$baseUrl/publish/v2/app-language-info", {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8))
                        append("client_id", clientId)
                        bearerAuth(token)
                    }
                    url {
                        parameters.append("appId", appId.getAppId())
                    }
                    setBody(HuaweiUpdateLanguageBody(newFeatures = description))
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiBaseResponse::class.java)
                        if (response.ret?.code == 0) {
                            updateLanguageInfoSuccessMap[packageName][description] = ""
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
        }
    }

    private fun uploadApk(
        apk: Apk, onFail: ((String) -> Unit)? = null, callback: (HuaweiUploadFileResponse) -> Unit
    ) {
        uploadApk(apk, object : SuccessCallback<HuaweiUploadFileResponse> {
            override fun onSuccess(t: HuaweiUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadApkMap = autoSaveMap("huaweiUploadApk") { mutableMapOf<String, HuaweiUploadFileResponse>() }

    //更新apk
    private fun uploadApk(apk: Apk, callback: SuccessCallback<HuaweiUploadFileResponse>) {
        val successResult = uploadApkMap[apk.packageName]
        if (successResult != null) {
            updateAppFileInfo(apk.packageName, HuaweiUpdateFileInfoBody(
                5, listOf(
                    HuaweiFileInfoBody(
                        fileName = apk.fileName,
                        fileDestUrl = successResult.result?.UploadFileRsp?.fileInfoList?.getOrNull(0)?.fileDestUlr ?: ""
                    )
                )
            ), { callback.onFail(it) }) {
                callback.onSuccess(successResult)
            }
            return
        }
        getAppId(apk.packageName, { callback.onFail(it) }) { appId ->
            getUploadUrl(apk.packageName, appId.getAppId(), "apk", apk.filePath, { callback.onFail(it) }) { uploadUrl ->
                Http.post(uploadUrl.uploadUrl, {
                    timeout {
                        requestTimeoutMillis = 120000
                    }
                    setBody(MultiPartFormDataContent(formData {
                        append("authCode", uploadUrl.authCode)
                        append("fileCount", 1)
                        append("file", File(apk.filePath).readBytes(), apkHeader(apk.filePath))
                    }))
                    var lastProcess = 0L
                    onUpload { bytesSentTotal, contentLength ->
                        if (contentLength == null) return@onUpload
                        val curProcess = bytesSentTotal * 100 / contentLength
                        if (curProcess != lastProcess) {
                            lastProcess = curProcess
                            println("文件:${apk.fileName} 上传进度:${curProcess}%")
                        }
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiUploadFileResponse::class.java)
                        if (response.result?.UploadFileRsp?.ifSuccess == 1) {
                            updateAppFileInfo(apk.packageName, HuaweiUpdateFileInfoBody(
                                5, listOf(
                                    HuaweiFileInfoBody(
                                        fileName = apk.fileName,
                                        fileDestUrl = response.result.UploadFileRsp.fileInfoList?.getOrNull(0)?.fileDestUlr
                                            ?: ""
                                    )
                                )
                            ), { callback.onFail(it) }) {
                                uploadApkMap[apk.packageName] = response
                                callback.onSuccess(response)
                            }
                        } else {
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }
                })
            }
        }
    }

    private fun uploadScreenShotList(
        uploadData: UploadData, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit
    ) {
        uploadScreenShotList(uploadData, callback = object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    //更新截图
    private fun uploadScreenShotList(
        uploadData: UploadData,
        uploadPos: Int = 0,
        screenShotResultList: MutableList<HuaweiUploadFileResponse>? = null,
        callback: SuccessCallback<String>
    ) {
        if (uploadData.imageList.isNullOrEmpty()) {
            callback.onSuccess("")
            return
        }
        val uploadScreenShotResult = screenShotResultList ?: mutableListOf()
        val image = uploadData.imageList[uploadPos]
        uploadScreenShot(uploadData.packageName()!!, image, { callback.onFail(it) }) { uploadFile ->
            uploadScreenShotResult.add(uploadFile)
            if (uploadScreenShotResult.size == uploadData.imageList.size) {
                updateAppFileInfo(uploadData.packageName()!!,
                    HuaweiUpdateFileInfoBody(2, uploadScreenShotResult.map { upload ->
                        HuaweiFileInfoBody(
                            upload.result?.UploadFileRsp?.fileInfoList?.getOrNull(0)?.fileDestUlr ?: ""
                        )
                    }),
                    onFail = { callback.onFail(it) }) {
                    callback.onSuccess("")
                }
            } else {
                uploadScreenShotList(uploadData, uploadPos + 1, uploadScreenShotResult, callback)
                println("正在上传截图，目前已成功${uploadScreenShotResult.size}个,总共${uploadData.imageList.size}个")
            }
        }
    }

    private fun uploadScreenShot(
        packageName: String,
        screenShot: String,
        onFail: ((String) -> Unit)? = null,
        callback: (HuaweiUploadFileResponse) -> Unit
    ) {
        uploadScreenShot(packageName, screenShot, object : SuccessCallback<HuaweiUploadFileResponse> {
            override fun onSuccess(t: HuaweiUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadScreenShotMap = autoSaveMap("huaweiScreenShotMap") { (mutableMapOf<String, HuaweiUploadFileResponse>()) }

    private fun uploadScreenShot(
        packageName: String, screenShotPath: String, callback: SuccessCallback<HuaweiUploadFileResponse>
    ) {
        val successResult = uploadScreenShotMap[packageName + screenShotPath]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getAppId(packageName, { callback.onFail(it) }) { appId ->
            getUploadUrl(packageName, appId.getAppId(),
                File(screenShotPath).extension,
                screenShotPath,
                { callback.onFail(it) }) { uploadUrl ->
                Http.post(uploadUrl.uploadUrl, {
                    timeout {
                        requestTimeoutMillis = 60000
                    }
                    setBody(MultiPartFormDataContent(formData {
                        append("authCode", uploadUrl.authCode)
                        append("fileCount", 1)
                        append("parseType", 1)
                        append("file", File(screenShotPath).readBytes(), jpgHeader(screenShotPath))
                    }))
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiUploadFileResponse::class.java)
                        if (response.result?.UploadFileRsp?.ifSuccess == 1) {
                            uploadScreenShotMap[packageName + screenShotPath] = response
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
        }
    }

    private fun updateAppFileInfo(
        packageName: String,
        infoBody: HuaweiUpdateFileInfoBody,
        onFail: ((String) -> Unit)? = null,
        callback: (String) -> Unit
    ) {
        updateAppFileInfo(packageName, infoBody, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun updateAppFileInfo(
        packageName: String, infoBody: HuaweiUpdateFileInfoBody, callback: SuccessCallback<String>
    ) {
        getToken(packageName, { callback.onFail(it) }) { token ->
            getAppId(packageName, { callback.onFail(it) }) { appId ->
                Http.put("$baseUrl/publish/v2/app-file-info", {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8))
                        append("client_id", clientId)
                        bearerAuth(token)
                    }
                    url {
                        parameters.append("appId", appId.getAppId())
                        parameters.append("releaseType", "1")
                    }
                    setBody(infoBody)
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, HuaweiBaseResponse::class.java)
                        if (response.ret?.code == 0) {
                            callback.onSuccess("")
                        } else {
//                            clearScreenShotMap()
                            callback.onFail("")
                        }
                    }

                    override fun onFail(msg: String) {
                        callback.onFail(msg)
                    }

                })
            }
        }
    }

    private fun getUploadUrl(
        packageName: String,
        appId: String,
        suffix: String = "apk",
        key: String,
        onFail: ((String) -> Unit)? = null,
        callback: (HuaweiUploadUrlResponse) -> Unit
    ) {
        getUploadUrl(packageName, appId, suffix, key, callback = object : SuccessCallback<HuaweiUploadUrlResponse> {
            override fun onSuccess(t: HuaweiUploadUrlResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadUrlMap = autoSaveMap("huaweiUploadUrl") { mutableMapOf<String, HuaweiUploadUrlResponse>() }
    private fun getUploadUrl(
        packageName: String,
        appId: String, suffix: String = "apk", key: String, callback: SuccessCallback<HuaweiUploadUrlResponse>
    ) {
        val successResult = uploadUrlMap[key]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getToken(packageName, { callback.onFail(it) }) { token ->
            Http.get("$baseUrl/publish/v2/upload-url", {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    append("client_id", clientId)
                    bearerAuth(token)
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
                        uploadUrlMap[key] = response
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
    }

    private fun getAppId(
        packageName: String, onFail: ((String) -> Unit)? = null, callback: (HuaweiAppIdResponse) -> Unit
    ) {
        getAppId(packageName, object : SuccessCallback<HuaweiAppIdResponse> {
            override fun onSuccess(t: HuaweiAppIdResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val appIdMap = autoSaveMap("huaweiAppId") { mutableMapOf<String, HuaweiAppIdResponse>() }
    private fun getAppId(packageName: String, callback: SuccessCallback<HuaweiAppIdResponse>) {
        val appId = appIdMap[packageName]
        if (appId != null) {
            callback.onSuccess(appId)
            return
        }
        getToken(packageName, { callback.onFail(it) }) {
            Http.get("$baseUrl/publish/v2/appid-list", request = {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    append("client_id", clientId)
                    bearerAuth(it)
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
    }

    private fun getToken(packageName: String, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit) {
        getToken(packageName, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private var token = mutableMapOf<String, MutableMap<Long, String>>()

    private fun getEfficientToken(packageName: String): String {
        var efficientToken = ""
        val noEfficientToken = mutableListOf<Long>()
        token[packageName]?.forEach { (t, u) ->
            if ((System.currentTimeMillis() / 1000) - t < 172800) {
                efficientToken = u
            } else {
                noEfficientToken.add(t)
            }
        }
        noEfficientToken.forEach {
            token[packageName]?.remove(it)
        }
        return efficientToken
    }

    private fun getToken(packageName: String, callback: SuccessCallback<String>) {
        val efficientToken = getEfficientToken(packageName)
        if (efficientToken.isNotEmpty()) {
            callback.onSuccess(efficientToken)
            return
        }
        Http.post("$baseUrl/oauth2/v1/token", request = {
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
                    token.getOrPut(packageName) {
                        mutableMapOf()
                    }[System.currentTimeMillis() / 1000] = response.access_token
//                    token.saveData()
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

    override fun clearCache() {
        super.clearCache()
        token.clear()
        appIdMap.clear()
        uploadUrlMap.clear()
        uploadScreenShotMap.clear()
        uploadApkMap.clear()
        updateLanguageInfoSuccessMap.clear()
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
    val imgShowType: Int = 0,
    val deviceType: Int = 4,
    val lang: String = "zh-CN",
)

@Serializable
internal data class HuaweiFileInfoBody(
    val fileDestUrl: String,
    val fileName: String? = null,           //文件名称，包括文件的后缀名。 fileType为5时，此参数必选。
)

@Serializable
internal data class HuaweiUploadFileResponse(
    val result: UploadFileResult? = null
)

@Serializable
internal data class UploadFileResult(
    val resultCode: String? = null, val UploadFileRsp: UploadFileRsp? = null
)

@Serializable
internal data class UploadFileRsp(
    val ifSuccess: Int? = 0, val fileInfoList: List<UploadFileInfo>? = null
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
) {
    fun isSuccess(): Boolean = ret?.code == 0
}

@Serializable
internal data class BaseResponse(
    val code: Int = 0,
    val msg: String? = null,
)