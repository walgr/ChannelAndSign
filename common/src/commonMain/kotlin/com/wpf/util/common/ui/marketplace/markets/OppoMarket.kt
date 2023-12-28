package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.gson.reflect.TypeToken
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.markets.base.*
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.Charset
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class OppoMarket(
    var clientId: String = "", var clientSecret: String = ""
) : Market {

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = MarketType.Oppo.channelName

    @Transient
    override val baseUrl: String = "https://oop-openapi-cn.heytapmobi.com"

    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is OppoMarket) return
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

    override fun query(uploadData: UploadData, callback: Callback<MarketType>) {
        super.query(uploadData, callback)
        if (uploadData.packageName().isNullOrEmpty()) return
        getAppInfo(uploadData.packageName()!!, { callback.onFail(it) }) {
            callback.onSuccess(MarketType.Oppo)
        }
    }

    private fun getAppInfo(
        packageName: String, onFail: ((String) -> Unit)? = null, callback: (OppoAppInfoData) -> Unit
    ) {
        getAppInfo(packageName, object : SuccessCallback<OppoAppInfoData> {
            override fun onSuccess(t: OppoAppInfoData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun getAppInfo(packageName: String, callback: SuccessCallback<OppoAppInfoData>) {
        getToken(packageName) { token ->
            Http.get("$baseUrl/resource/v1/app/info", {
                url {
                    val parameterList = getCommonParams(packageName, token).plus(
                        mapOf("pkg_name" to packageName)
                    )
                    parameterList.forEach { (t, u) ->
                        parameters.append(t, u)
                    }
                    parameters.append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }
            }, callback = object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoAppInfoData>>(
                        t, object : TypeToken<OppoBaseResponse<OppoAppInfoData>>() {}.type
                    )
                    if (response.isSuccess() && response.data != null) {
                        callback.onSuccess(response.data)
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

    private fun getMultiAppInfo(
        packageName: String, onFail: ((String) -> Unit)? = null, callback: (OppoAppInfoData) -> Unit
    ) {
        getMultiAppInfo(packageName, object : SuccessCallback<OppoAppInfoData> {
            override fun onSuccess(t: OppoAppInfoData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun getMultiAppInfo(packageName: String, callback: SuccessCallback<OppoAppInfoData>) {
        getToken(packageName) { token ->
            Http.get("$baseUrl/resource/v1/app/multi-info", {
                url {
                    val parameterList = getCommonParams(packageName, token).plus(
                        mapOf("pkg_name" to packageName)
                    )
                    parameterList.forEach { (t, u) ->
                        parameters.append(t, u)
                    }
                    parameters.append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }
            }, callback = object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoAppInfoData>>(
                        t, object : TypeToken<OppoBaseResponse<OppoAppInfoData>>() {}.type
                    )
                    if (response.isSuccess() && response.data != null) {
                        callback.onSuccess(response.data)
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

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.packageName().isNullOrEmpty()) return
        getMultiAppInfo(uploadData.packageName()!!, { callback.onFail(it) }) { appInfo ->
            uploadFiles(uploadData.packageName()!!, uploadData.apk.abiApk.map { it.filePath }, "apk", {
                callback.onFail("${name}:${it}")
            }) { uploadApkList ->
                uploadFiles(uploadData.packageName()!!, uploadData.imageList, "photo", {
                    callback.onFail("${name}:${it}")
                }) { uploadPhotoList ->
                    upload(uploadData, appInfo, uploadApkList, uploadPhotoList, {
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(10000)
                            getTaskStateResult(uploadData.packageName()!!, uploadData.versionCode()!!) {
                                if (it.task_state == "2") {
                                    callback.onSuccess(MarketType.Oppo)
                                } else {
                                    callback.onFail("${name}:${it.err_msg}")
                                }
                            }
                        }
                    }) {
                        callback.onSuccess(MarketType.Oppo)
                    }
                }
            }
        }
    }

    private fun upload(
        uploadData: UploadData,
        appInfo: OppoAppInfoData,
        uploadApkList: List<OppoUploadFileData>,
        uploadPhotoList: List<OppoUploadFileData>,
        onFail: ((String) -> Unit)? = null,
        callback: (OppoUpdateResponse) -> Unit
    ) {
        upload(uploadData, appInfo, uploadApkList, uploadPhotoList, object : SuccessCallback<OppoUpdateResponse> {
            override fun onSuccess(t: OppoUpdateResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val abiMap = mapOf(AbiType.Abi32 to 32, AbiType.Abi64 to 64, AbiType.Abi32_64 to 0)
    private fun upload(
        uploadData: UploadData,
        appInfo: OppoAppInfoData,
        uploadApkList: List<OppoUploadFileData>,
        uploadPhotoList: List<OppoUploadFileData>,
        callback: SuccessCallback<OppoUpdateResponse>
    ) {
        getToken(uploadData.packageName()!!) { token ->
            Http.post("$baseUrl/resource/v1/app/upd", {
                setBody(MultiPartFormDataContent(formData {
                    val parameterList = getCommonParams(uploadData.packageName()!!, token).plus(
                        mapOf(
                            "pkg_name" to uploadData.packageName()!!,
                            "version_code" to uploadData.versionCode()!!,
                            "apk_url" to gson.toJson(uploadApkList.map { uploadFile ->
                                OppoAppInfo(
                                    uploadFile.url, uploadFile.md5, abiMap[uploadData.apk.abiApk.find {
                                        it.filePath == uploadFile.filePath
                                    }!!.abi]
                                )
                            }),
                            "update_desc" to uploadData.description,
                            "online_type" to "1",
                            "pic_url" to uploadPhotoList.map {
                                it.url
                            }.joinToString(",")
                        )
                    ).toMutableMap()
                    parameterList["test_desc"] = uploadData.leaveMessage ?: ""
                    parameterList["second_category_id"] = appInfo.second_category_id
                    parameterList["third_category_id"] = appInfo.third_category_id
                    parameterList["summary"] = appInfo.summary
                    parameterList["detail_desc"] = appInfo.detail_desc
                    parameterList["privacy_source_url"] = appInfo.privacy_source_url
                    parameterList["icon_url"] = appInfo.icon_url
                    parameterList["pic_url"] = appInfo.pic_url
                    parameterList["copyright_url"] = appInfo.copyright_url
                    parameterList["age_level"] = appInfo.age_level
                    parameterList["adaptive_equipment"] = appInfo.adaptive_equipment
                    parameterList.forEach { (t, u) ->
                        append(t, u)
                    }
                    append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }))
            }, callback = object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoUpdateResponse>>(
                        t, object : TypeToken<OppoBaseResponse<OppoUpdateResponse>>() {}.type
                    )
                    if (response.isSuccess() && response.data != null) {
                        callback.onSuccess(response.data)
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

    private fun getTaskStateResult(
        packageName: String,
        versionCode: String,
        onFail: ((String) -> Unit)? = null,
        callback: (OppoTaskStateData) -> Unit
    ) {
        getTaskState(packageName, versionCode, {
            CoroutineScope(Dispatchers.Main).launch {
                delay(2000)
                getTaskStateResult(packageName, versionCode, onFail, callback)
            }
        }, callback)
    }

    private fun getTaskState(
        packageName: String,
        versionCode: String,
        onFail: ((String) -> Unit)? = null,
        callback: (OppoTaskStateData) -> Unit
    ) {
        getTaskState(packageName, versionCode, object : SuccessCallback<OppoTaskStateData> {
            override fun onSuccess(t: OppoTaskStateData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun getTaskState(packageName: String, versionCode: String, callback: SuccessCallback<OppoTaskStateData>) {
        getToken(packageName) { token ->
            Http.post("$baseUrl/resource/v1/app/task-state", {
                setBody(MultiPartFormDataContent(formData {
                    val parameterList = getCommonParams(packageName, token).plus(
                        mapOf("pkg_name" to packageName, "version_code" to versionCode)
                    )
                    parameterList.forEach { (t, u) ->
                        append(t, u)
                    }
                    append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }))
            }, object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoTaskStateData>>(
                        t, object : TypeToken<OppoBaseResponse<OppoTaskStateData>>() {}.type
                    )
                    if (response.isSuccess() && response.data != null) {
                        callback.onSuccess(response.data)
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

    private fun uploadFiles(
        packageName: String,
        filePathList: List<String>?,
        type: String,
        onFail: ((String) -> Unit)? = null,
        callback: (List<OppoUploadFileData>) -> Unit
    ) {
        uploadFiles(packageName, filePathList, type, callback = object : SuccessCallback<List<OppoUploadFileData>> {
            override fun onSuccess(t: List<OppoUploadFileData>) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadFiles(
        packageName: String,
        filePathList: List<String>?,
        type: String,
        successResultList: MutableList<OppoUploadFileData>? = null,
        callback: SuccessCallback<List<OppoUploadFileData>>
    ) {
        if (filePathList.isNullOrEmpty()) {
            callback.onSuccess(arrayListOf())
            return
        }
        getToken(packageName) {
            if (successResultList == null) {
                println("需要上传${filePathList.size}个")
            }
            val successResult = successResultList ?: mutableListOf()
            val filePath = filePathList[successResult.size]
            uploadFile(packageName, filePath, type, { error -> callback.onFail(error) }) { uploadFile ->
                successResult.add(uploadFile)
                if (successResult.size == filePathList.size) {
                    callback.onSuccess(successResult)
                } else {
                    println("当前:${filePath}上传成功， 还有${filePathList.size - successResult.size}个")
                    uploadFiles(packageName, filePathList, type, successResult, callback)
                }
            }
        }
    }

    /**
     * @param type 文件类型，包括照片、APK 包、其它，值是：photo、apk、resource
     */
    private fun uploadFile(
        packageName: String,
        filePath: String,
        type: String,
        onFail: ((String) -> Unit)? = null,
        callback: (OppoUploadFileData) -> Unit
    ) {
        uploadFile(packageName, filePath, type, object : SuccessCallback<OppoUploadFileData> {
            override fun onSuccess(t: OppoUploadFileData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadFileMap = autoSaveMap("${name}UploadFileMap") { mutableMapOf<String, OppoUploadFileData>() }

    private fun uploadFile(
        packageName: String, filePath: String, type: String, callback: SuccessCallback<OppoUploadFileData>
    ) {
        val successResult = uploadFileMap[filePath]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getToken(packageName, { callback.onFail(it) }) { token ->
            getUploadUrl(packageName, filePath, { callback.onFail(it) }) {
                Http.post("${it.upload_url}", {
                    timeout {
                        requestTimeoutMillis = 300000
                    }
                    setBody(MultiPartFormDataContent(formData {
                        val parameterList = getCommonParams(packageName, token).plus(
                            mapOf("type" to type, "sign" to it.sign!!)
                        )
                        parameterList.forEach { (t, u) ->
                            append(t, u)
                        }
                        append("file", File(filePath).readBytes(), fileHeader(filePath))
                        append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                    }))
                    var lastProcess = 0L
                    onUpload { bytesSentTotal, contentLength ->
                        val curProcess = bytesSentTotal * 100 / contentLength
                        if (curProcess != lastProcess) {
                            lastProcess = curProcess
                            println("文件:${filePath} 上传进度:${curProcess}%")
                        }
                    }
                }, callback = object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson<OppoBaseResponse<OppoUploadFileData>>(
                            t, object : TypeToken<OppoBaseResponse<OppoUploadFileData>>() {}.type
                        )
                        if (response.isSuccess() && response.data != null) {
                            response.data.filePath = filePath
                            uploadFileMap[filePath] = response.data
                            callback.onSuccess(response.data)
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

    private fun getUploadUrl(
        packageName: String, key: String, onFail: ((String) -> Unit)? = null, callback: (OppoUploadUrlData) -> Unit
    ) {
        getUploadUrl(packageName, key, object : SuccessCallback<OppoUploadUrlData> {
            override fun onSuccess(t: OppoUploadUrlData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadUrlMap = autoSaveMap("${name}UploadUrl") { mutableMapOf<String, OppoUploadUrlData>() }
    private fun getUploadUrl(packageName: String, key: String, callback: SuccessCallback<OppoUploadUrlData>) {
        val successResult = uploadUrlMap[key]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getToken(packageName, { callback.onFail(it) }) { token ->
            Http.get("$baseUrl/resource/v1/upload/get-upload-url", {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                }
                url {
                    val parameterList = getCommonParams(packageName, token)
                    parameterList.forEach { (t, u) ->
                        parameters.append(t, u)
                    }
                    parameters.append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }
            }, object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoUploadUrlData>>(
                        t, object : TypeToken<OppoBaseResponse<OppoUploadUrlData>>() {}.type
                    )
                    if (response.isSuccess() && response.data != null) {
                        uploadUrlMap[key] = response.data
                        callback.onSuccess(response.data)
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

    private fun getToken(packageName: String, callback: SuccessCallback<String>) {
        val efficientToken = getEfficientToken(packageName)
        if (efficientToken.isNotEmpty()) {
            callback.onSuccess(efficientToken)
            return
        }
        Http.get("$baseUrl/developer/v1/token", {
            url {
                parameters.append("client_id", clientId)
                parameters.append("client_secret", clientSecret)
            }
        }, object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson<OppoBaseResponse<OppoTokenData>>(
                    t, object : TypeToken<OppoBaseResponse<OppoTokenData>>() {}.type
                )
                if (response.isSuccess() && !response.data?.access_token.isNullOrEmpty()) {
                    token[packageName][System.currentTimeMillis() / 1000] = response.data?.access_token!!
                    token.saveData()
                    callback.onSuccess(response.data.access_token)
                } else {
                    callback.onFail("")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }

        })
    }

    //过期时间
    @Transient
    private val extTime = 48 * 3600

    @Transient
    private var token = autoSaveMap("${name}Token") { mutableMapOf<String, MutableMap<Long, String>>() }

    private fun getEfficientToken(packageName: String): String {
        var efficientToken = ""
        val noEfficientToken = mutableListOf<Long>()
        token[packageName].forEach { (t, u) ->
            if ((System.currentTimeMillis() / 1000) - t < extTime) {
                efficientToken = u
            } else {
                noEfficientToken.add(t)
            }
        }
        noEfficientToken.forEach {
            token[packageName].remove(it)
        }
        return efficientToken
    }

    private fun getCommonParams(packageName: String, accessToken: String? = null) = mutableMapOf(
        Pair("access_token", accessToken ?: getEfficientToken(packageName)),
        Pair("timestamp", (System.currentTimeMillis() / 1000).toString()),
    ).filterValues {
        it.isNotEmpty()
    }.toMutableMap()

    /**
     * 根据传入的map，把map里的key   value转换为接口的请求参数，并给参数按ascii码排序
     * @param paramsMap 传入的map
     * @return 按ascii码排序的参数键值对拼接结果
     */
    private fun getUrlParamsFromMap(paramsMap: Map<String, Any>): String {
        val keysList: MutableList<String> = paramsMap.keys.toMutableList()
        keysList.sort()
        val paramList: MutableList<String> = ArrayList()
        for (key in keysList) {
            val obj = paramsMap[key] ?: continue
            val value = "$key=$obj"
            paramList.add(value)
        }
        return paramList.joinToString("&")
    }


    /**
     * HMAC_SHA256 验签加密
     * @param data 需要加密的参数
     * @param key 签名密钥
     * @return String 返回加密后字符串
     */
    private fun hmacSHA256(data: String, key: String): String {
        try {
            val secretByte = key.toByteArray(Charset.forName("UTF-8"))
            val signingKey = SecretKeySpec(secretByte, "HmacSHA256")
            val mac: Mac = Mac.getInstance("HmacSHA256")
            mac.init(signingKey)
            val dataByte = data.toByteArray(Charset.forName("UTF-8"))
            val by: ByteArray = mac.doFinal(dataByte)
            return byteArr2HexStr(by)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /**
     * HMAC_SHA256加密后的数组进行16进制转换
     */
    private fun byteArr2HexStr(bytes: ByteArray): String {
        val length = bytes.size
        //每个byte用两个字符才能表示，所以字符串的长度是数组长度的两倍
        val sb = StringBuilder(length * 2)
        for (i in 0 until length) {
            //将得到的字节转16进制
            val strHex = Integer.toHexString(bytes[i].toInt() and 0xFF)
            // 每个字节由两个字符表示，位数不够，高位补0
            sb.append(if (strHex.length == 1) "0$strHex" else strHex)
        }
        return sb.toString()
    }

    override fun clearCache() {
        super.clearCache()
        token.clear()
        uploadUrlMap.clear()
        uploadFileMap.clear()
    }
}

@Serializable
internal data class OppoUpdateResponse(
    val success: Boolean? = null,
    val message: String? = null,
)

@Serializable
internal data class OppoUpdateBody(
    val pkg_name: String? = null,
    val version_code: String? = null,
    val update_desc: String? = null,            //版本说明，不少于 5 个字
    val apk_url: List<OppoAppInfo>? = null,
    val pic_url: List<String>? = null,          //竖版截图 url，多个用英文逗号分隔，不能少于两张，上传 3-5 张截图，支持 jpg、png 格式。截图尺寸要求：1080*1920，单张图片不能超过 1M
)

@Serializable
internal data class OppoAppInfo(
    val url: String? = null,
    val md5: String? = null,
    val cpu_code: Int? = null,      //多包平台，64 位 CPU 包为 64，32 位 CPU 包为 32，非多包应用为 0
)

@Serializable
internal data class OppoTaskStateData(
    val pkg_name: String? = null,
    val version_code: String? = null,
    val task_state: String? = null,     //状态，1-待处理；2-处理成功；3-处理失败
    val err_msg: String? = null,
)

@Serializable
internal data class OppoUploadFileData(
    val id: String? = null,
    val url: String? = null,
    val uri_path: String? = null,
    val md5: String? = null,
    val file_extension: String? = null,
    val file_size: String? = null,
    val width: Int? = null,
    val height: Int? = null,

    @kotlinx.serialization.Transient var filePath: String? = null
)

@Serializable
internal data class OppoUploadUrlData(
    val upload_url: String? = null,
    val sign: String? = null,
)

@Serializable
internal data class OppoAppInfoData(
    val app_id: String? = null,
    val pkg_name: String? = null,
    val type: Int? = null,
    val sign: String? = null,
    val dev_id: String? = null,
    val app_key: String? = null,
    val app_secret: String? = null,

    val second_category_id: String? = null,
    val third_category_id: String? = null,
    val summary: String? = null,
    val detail_desc: String? = null,
    val privacy_source_url: String? = null,
    val icon_url: String? = null,
    val pic_url: String? = null,
    val copyright_url: String? = null,
    val age_level: String? = null,
    val adaptive_equipment: String? = null,
)

@Serializable
internal data class OppoTokenData(
    val access_token: String? = null
)

@Serializable
internal data class OppoBaseResponse<T>(
    val errno: Int? = null,
    val data: T? = null,
) {
    fun isSuccess() = errno == 0
}