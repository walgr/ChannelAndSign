package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.markets.base.*
import com.wpf.util.common.ui.utils.Callback
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


data class VivoMarket(
    var accessKey: String = "",
    var accessSecret: String = ""
) : Market {

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = MarketType.Vivo.channelName

    @Transient
    override val baseUrl: String = "https://sandbox-developer-api.vivo.com.cn/router/rest"

    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    override fun query(uploadData: UploadData) {
        super.query(uploadData)
        query(uploadData.packageName()!!)
    }

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.packageName().isNullOrEmpty()) return
        val packageName = uploadData.packageName()!!
        val versionCode = uploadData.versionCode()!!
        //先上传apk和相应的图片资源
        val uploadList = uploadData.apk.abiApk.filter {
            uploadAbi().contains(it.abi)
        }
        if (uploadList.size != uploadAbi().size) {
            println("缺少相关Apk,不能上传,请检查，当前符合包：${uploadList.map { it.abi.type }}")
            return
        }
        var uploadSuccess = 0
        val uploadResultList = arrayListOf<VivoResponse>()
        println("上传Apk中，需要上传:${uploadList.map { it.abi.type }} ${uploadList.size}个Apk")
        uploadList.forEach {
            uploadApk(it, callback = object : Callback<VivoResponse> {
                override fun onSuccess(t: VivoResponse) {
                    uploadResultList.add(t)
                    println("第${uploadResultList.size}个Apk上传完成")
                    uploadSuccess++
                    if (uploadSuccess == uploadList.size) {
                        println("Apk全部上传完成")
                        uploadScreenShotList(packageName, uploadData.imageList, object : Callback<List<VivoResponse>?> {
                            override fun onSuccess(t: List<VivoResponse>?) {
                                update(packageName,
                                    versionCode,
                                    getSerialnumber(uploadResultList),
                                    getFileMd5(uploadResultList),
                                    uploadData.description,
                                    screenshot = t?.map { response ->
                                        response.data?.serialnumber
                                    }?.joinToString(separator = ",") ?: "",
                                    callback = object : Callback<String> {
                                        override fun onSuccess(t: String) {
                                            println("Vivo接口请求成功,结果:$t")
                                        }

                                        override fun onFail(msg: String) {
                                            println("Vivo接口请求失败,结果:$msg")
                                        }
                                    })
                            }

                            override fun onFail(msg: String) {

                            }
                        })
                    }
                }

                override fun onFail(msg: String) {

                }
            })
        }
    }

    private fun getSerialnumber(uploadResultList: List<VivoResponse>): String {
        return uploadResultList.joinToString(separator = ",") {
            it.data?.serialnumber ?: ""
        }
    }

    private fun getFileMd5(uploadResultList: List<VivoResponse>): String {
        return uploadResultList.joinToString(separator = ",") {
            it.data?.fileMd5 ?: ""
        }
    }

    private fun update(
        packageName: String,
        versionCode: String,
        serialnumber: String,
        fileMd5: String,
        updateDesc: String,
        screenshot: String? = null,
        callback: Callback<String>
    ) {
        Http.submitForm(baseUrl, formParameters = parameters {
            val paramsMap = getUpdateParams(packageName, versionCode, serialnumber, fileMd5, updateDesc, screenshot)
            paramsMap.forEach { (t, u) ->
                append(t, u.toString())
            }
            append("sign", hmacSHA256(getUrlParamsFromMap(paramsMap), accessSecret))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, VivoResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess("上传成功")
                } else {
                    callback.onFail(response.msg ?: "")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }

    private fun uploadScreenShotList(
        packageName: String, screenShotPathList: List<String>?, callback: Callback<List<VivoResponse>?>
    ) {
        if (screenShotPathList.isNullOrEmpty()) {
            println("没有截图需要上传")
            callback.onSuccess(null)
            return
        }
        println("需要上传${screenShotPathList.size}个图片")
        val uploadResultList = arrayListOf<VivoResponse>()
        screenShotPathList.map {
            uploadScreenShot(packageName, it, callback = object : Callback<VivoResponse> {
                override fun onSuccess(t: VivoResponse) {
                    uploadResultList.add(t)
                    println("上传成功${uploadResultList.size}个图片")
                    if (uploadResultList.size == screenShotPathList.size) {
                        callback.onSuccess(uploadResultList)
                    }
                }

                override fun onFail(msg: String) {
                    callback.onFail(msg)
                }
            })
        }
    }

    private fun uploadScreenShot(packageName: String, screenShotPath: String, callback: Callback<VivoResponse>) {
        Http.post(baseUrl, request = {
            setBody(MultiPartFormDataContent(formData {
                val paramsMap = getScreenShotParams(packageName)
                paramsMap.toSortedMap().forEach { (t, u) ->
                    append(t, u)
                }
                append("file", File(screenShotPath).readBytes(), pngHeader(screenShotPath))
                append("sign", hmacSHA256(getUrlParamsFromMap(paramsMap), accessSecret))
            }))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, VivoResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess(response)
                } else {
                    callback.onFail(response.msg ?: "")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }

    private fun uploadApk(apk: Apk, callback: Callback<VivoResponse>) {
        Http.post(baseUrl, request = {
            timeout {
                requestTimeoutMillis = 300000
            }
            setBody(MultiPartFormDataContent(formData {
                val paramsMap = when (apk.abi) {
                    AbiType.Abi64 -> {
                        getUploadApk64Params(apk)
                    }

                    AbiType.Abi32 -> {
                        getUploadApk32Params(apk)
                    }

                    else -> {
                        getUploadApkAllParams(apk)
                    }
                }
                paramsMap.forEach { (t, u) ->
                    append(t, u)
                }
                append("sign", hmacSHA256(getUrlParamsFromMap(paramsMap), accessSecret))
                append("file", File(apk.filePath).readBytes(), apkHeader(apk.filePath))
            }))
            var lastProcess = 0L
            onUpload { bytesSentTotal, contentLength ->
                val curProcess = bytesSentTotal * 100 / contentLength
                if (curProcess != lastProcess) {
                    lastProcess = curProcess
                    println("上传进度:${curProcess}%")
                }
            }
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, VivoResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess(response)
                } else {
                    callback.onFail(response.msg ?: "")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }

    private fun query(packageName: String) {
        Http.submitForm(baseUrl, formParameters = parameters {
            val paramsMap = getQueryParams(packageName)
            paramsMap.forEach { (t, u) ->
                append(t, u)
            }
            append("sign", hmacSHA256(getUrlParamsFromMap(paramsMap), accessSecret))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                println("Vivo接口请求成功,结果:$t")
            }

            override fun onFail(msg: String) {
                println("Vivo接口请求失败,结果:$msg")
            }
        })
    }

    @Transient private val methodScreenShot = "app.upload.screenshot"
    @Transient private val methodApkAll = "app.upload.apk.app"
    @Transient private val methodApk32 = "app.upload.apk.app.32"
    @Transient private val methodApk64 = "app.upload.apk.app.64"
    @Transient private val methodUpdate = "app.sync.update.app"
    @Transient private val methodQuery = "app.query.details"
    private fun getCommonParams(api: String) = mutableMapOf(
        Pair("access_key", accessKey),
        Pair("method", api),
        Pair("timestamp", System.currentTimeMillis().toString()),
        Pair("format", "json"),
        Pair("sign_method", "hmac"),
        Pair("target_app_key", "developer"),
        Pair("v", "1.0"),
    )

    private fun getQueryParams(packageName: String) = mutableMapOf(
        Pair("packageName", packageName),
    ).plus(
        getCommonParams(methodQuery)
    )

    private fun getScreenShotParams(packageName: String) = mutableMapOf(
        Pair("packageName", packageName),
    ).plus(
        getCommonParams(methodScreenShot)
    )

    private fun getUpdateParams(
        packageName: String,
        versionCode: String,
        serialnumber: String,
        fileMd5: String,
        updateDesc: String,
        screenshot: String? = null
    ) = mutableMapOf(
        Pair("packageName", packageName),
        Pair("versionCode", versionCode),
        Pair("apk", serialnumber),
        Pair("fileMd5", fileMd5),
        Pair("onlineType", 1),
        Pair("updateDesc", updateDesc),     //新版说明（长度要求，5~200个字符）
        Pair("screenshot", screenshot),     //截图文件 上传接口返回的流水号（3-5张）多个用逗号分隔
    ).plus(
        getCommonParams(methodUpdate)
    ).filterValues {
        it != null
    } as MutableMap<String, Any>

    private fun getUploadApkAllParams(apk: Apk) = mutableMapOf(
        Pair("packageName", apk.packageName),
        Pair("fileMd5", getFileMD5(apk.filePath)),
    ).plus(
        getCommonParams(methodApkAll)
    )

    private fun getUploadApk32Params(apk: Apk) = mutableMapOf(
        Pair("packageName", apk.packageName),
        Pair("fileMd5", getFileMD5(apk.filePath)),
    ).plus(
        getCommonParams(methodApk32)
    )

    private fun getUploadApk64Params(apk: Apk) = mutableMapOf(
        Pair("packageName", apk.packageName),
        Pair("fileMd5", getFileMD5(apk.filePath)),
    ).plus(
        getCommonParams(methodApk64)
    )

    @Throws(IOException::class)
    private fun getFileMD5(filePath: String): String {
        var fis: FileInputStream? = null
        return try {
            fis = FileInputStream(filePath)
            DigestUtils.md5Hex(fis)
        } finally {
            fis?.close()
        }
    }

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
        if (market !is VivoMarket) return
        val accessKey = remember { mutableStateOf(market.accessKey) }
        accessKey.value = market.accessKey
        val accessSecret = remember { mutableStateOf(market.accessSecret) }
        accessSecret.value = market.accessSecret
        Column {
            InputView(input = accessKey, hint = "请配置ACCESS_KEY") {
                accessKey.value = it
                market.accessKey = it
            }
            InputView(input = accessSecret, hint = "请配置ACCESS_SECRET") {
                accessSecret.value = it
                market.accessSecret = it
            }
        }
    }
}

@Serializable
internal data class VivoResponse(
    val code: Int? = null,
    val subCode: String? = null,
    val msg: String? = null,
    val data: Data? = null,
) {
    fun isSuccess() = subCode == "0"
}

@Serializable
internal data class Data(
    val packageName: String? = null,
    val serialnumber: String? = null,
    val versionCode: String? = null,
    val versionName: String? = null,
    val fileMd5: String? = null,
)