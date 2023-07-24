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
import com.wpf.util.common.ui.utils.Callback
import com.wpf.util.common.ui.utils.SuccessCallback
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.settings
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.Charset
import java.util.ArrayList
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

    override fun query(uploadData: UploadData) {
        super.query(uploadData)
        if (uploadData.packageName().isNullOrEmpty()) return
        getAppInfo(uploadData.packageName()!!) {

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
        getToken { token ->
            Http.get("$baseUrl/resource/v1/app/info", {
                url {
                    val parameterList = getCommonParams(token).plus(
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
        uploadFiles(uploadData.apk.abiApk.map { it.filePath }, "apk", {
            callback.onFail("${name}:${it}")
        }) { uploadApkList ->
            uploadFiles(uploadData.imageList, "photo", {
                callback.onFail("${name}:${it}")
            }) { uploadPhotoList ->
                upload(uploadData, uploadApkList, uploadPhotoList, {
                    getTaskStateResult(uploadData.packageName()!!, uploadData.versionCode()!!) {
                        if (it.task_state == "2") {
                            callback.onSuccess(MarketType.Oppo)
                        } else {
                            callback.onFail("${name}:${it.err_msg}")
                        }
                    }
                }) {
                    callback.onSuccess(MarketType.Oppo)
                }
            }
        }
    }

    private fun upload(
        uploadData: UploadData,
        uploadApkList: List<OppoUploadFileData>,
        uploadPhotoList: List<OppoUploadFileData>,
        onFail: ((String) -> Unit)? = null,
        callback: (OppoUpdateResponse) -> Unit
    ) {
        upload(uploadData, uploadApkList, uploadPhotoList, object : SuccessCallback<OppoUpdateResponse> {
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
        uploadApkList: List<OppoUploadFileData>,
        uploadPhotoList: List<OppoUploadFileData>,
        callback: SuccessCallback<OppoUpdateResponse>
    ) {
        getToken { token ->
            Http.post("$baseUrl/resource/v1/app/upd", {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                }
                formData {
                    val parameterList = getCommonParams(token).plus(
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
                            "pic_url" to uploadPhotoList.map {
                                it.url
                            }.joinToString(",")
                        )
                    )
                    parameterList.forEach { (t, u) ->
                        append(t, u)
                    }
                    append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }
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
            getTaskStateResult(packageName, versionCode, onFail, callback)
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
        getToken { token ->
            Http.post("$baseUrl/resource/v1/app/task-state", {
                formData {
                    val parameterList = getCommonParams(token).plus(
                        mapOf("pkg_name" to packageName, "version_code" to versionCode)
                    )
                    parameterList.forEach { (t, u) ->
                        append(t, u)
                    }
                    append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                }
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
        filePathList: List<String>?,
        type: String,
        onFail: ((String) -> Unit)? = null,
        callback: (List<OppoUploadFileData>) -> Unit
    ) {
        uploadFiles(filePathList, type, object : SuccessCallback<List<OppoUploadFileData>> {
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
        filePathList: List<String>?, type: String, callback: SuccessCallback<List<OppoUploadFileData>>
    ) {
        if (filePathList.isNullOrEmpty()) {
            callback.onSuccess(arrayListOf())
            return
        }
        getToken {
            val successResult = mutableListOf<OppoUploadFileData>()
            println("需要上传${filePathList.size}个")
            filePathList.forEach {
                uploadFile(it, type, { error -> callback.onFail(error) }) { uploadFile ->
                    successResult.add(uploadFile)
                    if (successResult.size == filePathList.size) {
                        callback.onSuccess(successResult)
                    } else {
                        println("当前:${it}上传成功， 还有${filePathList.size - successResult.size}个")
                    }
                }
            }
        }
    }

    /**
     * @param type 文件类型，包括照片、APK 包、其它，值是：photo、apk、resource
     */
    private fun uploadFile(
        filePath: String, type: String, onFail: ((String) -> Unit)? = null, callback: (OppoUploadFileData) -> Unit
    ) {
        uploadFile(filePath, type, object : SuccessCallback<OppoUploadFileData> {
            override fun onSuccess(t: OppoUploadFileData) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadFile(filePath: String, type: String, callback: SuccessCallback<OppoUploadFileData>) {
        getToken({ callback.onFail(it) }) { token ->
            getUploadUrl(filePath, { callback.onFail(it) }) {
                Http.post("${it.upload_url}", {
                    setBody(MultiPartFormDataContent(formData {
                        val parameterList = getCommonParams(token).plus(
                            mapOf("type" to type, "sign" to it.sign!!)
                        )
                        parameterList.forEach { (t, u) ->
                            append(t, u)
                        }
                        append("file", File(filePath).readBytes(), fileHeader(filePath))
                        append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                    }))
                }, callback = object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson<OppoBaseResponse<OppoUploadFileData>>(
                            t, object : TypeToken<OppoBaseResponse<OppoUploadFileData>>() {}.type
                        )
                        if (response.isSuccess() && response.data != null) {
                            response.data.filePath = filePath
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

    private fun getUploadUrl(key: Any, onFail: ((String) -> Unit)? = null, callback: (OppoUploadUrlData) -> Unit) {
        getUploadUrl(key, object : SuccessCallback<OppoUploadUrlData> {
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
    private val uploadUrlMap = mutableMapOf<Any, OppoUploadUrlData>()
    private fun getUploadUrl(key: Any, callback: SuccessCallback<OppoUploadUrlData>) {
        val successResult = uploadUrlMap[key]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getToken({ callback.onFail(it) }) { token ->
            Http.get("$baseUrl/resource/v1/upload/get-upload-url", {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
                }
                url {
                    val parameterList = getCommonParams(token)
                    url {
                        parameterList.forEach { (t, u) ->
                            parameters.append(t, u)
                        }
                        parameters.append("api_sign", hmacSHA256(getUrlParamsFromMap(parameterList), clientSecret))
                    }
                }
            }, object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson<OppoBaseResponse<OppoUploadUrlData>>(
                        t, object : TypeToken<OppoBaseResponse<OppoUploadUrlData>>() {}.type
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

    private fun getToken(onFail: ((String) -> Unit)? = null, callback: (String) -> Unit) {
        getToken(object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun getToken(callback: SuccessCallback<String>) {
        val efficientToken = getEfficientToken()
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
                    token[System.currentTimeMillis() / 1000] = response.data?.access_token!!
                    saveToken()
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
    private var token = gson.fromJson<MutableMap<Long, String>>(
        settings.getString("oppoToken", "{}"), object : TypeToken<MutableMap<Long, String>>() {}.type
    ) ?: mutableMapOf()

    private fun getEfficientToken(): String {
        var efficientToken = ""
        val noEfficientToken = mutableListOf<Long>()
        token.forEach { (t, u) ->
            if ((System.currentTimeMillis() / 1000) - t < extTime) {
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

    private fun saveToken() {
        settings.putString("oppoToken", gson.toJson(token))
    }

    private fun clearToken() {
        settings.putString("oppoToken", "")
    }

    private fun getCommonParams(accessToken: String? = null) = mutableMapOf(
        Pair("access_token", accessToken ?: getEfficientToken()),
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
    val app_secret: String? = null,
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