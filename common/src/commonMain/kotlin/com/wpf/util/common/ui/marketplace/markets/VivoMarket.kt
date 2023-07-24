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
import com.wpf.util.common.ui.utils.SuccessCallback
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
//    override val baseUrl: String = "https://developer-api.vivo.com.cn/router/rest"

    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    override fun query(uploadData: UploadData) {
        super.query(uploadData)
        query(uploadData.packageName()!!)
//        push(uploadData, object : Callback<MarketType> {
//            override fun onSuccess(t: MarketType) {
//                println("Vivo上传成功")
//            }
//
//            override fun onFail(msg: String) {
//                println("Vivo上传失败")
//            }
//
//        })
    }

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.packageName().isNullOrEmpty()) return
        val packageName = uploadData.packageName()!!
        val versionCode = uploadData.versionCode()!!
        uploadApkList(uploadData, { callback.onFail(it) }) { uploadResultList ->
            uploadScreenShotList(packageName,
                uploadData.imageList,
                { error -> callback.onFail(error) }) { screenFiles ->
                update(packageName,
                    versionCode,
                    uploadResultList.find { find -> find.abi == AbiType.Abi32 }?.data?.serialnumber!!,
                    uploadResultList.find { find -> find.abi == AbiType.Abi64 }?.data?.serialnumber!!,
                    uploadData.description,
                    screenshot = screenFiles.map { response ->
                        response.data?.serialnumber
                    }.joinToString(separator = ","),
                    { callback.onFail(it) }) {
                    callback.onSuccess(MarketType.Vivo)
                }
            }
        }
    }

    private fun update(
        packageName: String,
        versionCode: String,
        serialnumber32: String,
        serialnumber64: String,
        updateDesc: String,
        screenshot: String? = null,
        onFail: ((String) -> Unit)? = null,
        callback: (String) -> Unit
    ) {
        update(packageName,
            versionCode,
            serialnumber32,
            serialnumber64,
            updateDesc,
            screenshot,
            object : SuccessCallback<String> {
                override fun onSuccess(t: String) {
                    callback.invoke(t)
                }

                override fun onFail(msg: String) {
                    super.onFail(msg)
                    onFail?.invoke(msg)
                }
            })
    }

    private fun update(
        packageName: String,
        versionCode: String,
        serialnumber32: String,
        serialnumber64: String,
        updateDesc: String,
        screenshot: String? = null,
        callback: SuccessCallback<String>
    ) {
        Http.submitForm(baseUrl, formParameters = parameters {
            val paramsMap =
                getUpdateParams(packageName, versionCode, serialnumber32, serialnumber64, updateDesc, screenshot)
            paramsMap.forEach { (t, u) ->
                append(t, u.toString())
            }
            append("sign", hmacSHA256(getUrlParamsFromMap(paramsMap), accessSecret))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, VivoBaseResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess("")
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
        packageName: String,
        screenShotPathList: List<String>?,
        onFail: ((String) -> Unit)? = null,
        callback: (List<VivoUploadFileResponse>) -> Unit
    ) {
        uploadScreenShotList(packageName, screenShotPathList, object : SuccessCallback<List<VivoUploadFileResponse>> {
            override fun onSuccess(t: List<VivoUploadFileResponse>) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadScreenShotList(
        packageName: String, screenShotPathList: List<String>?, callback: SuccessCallback<List<VivoUploadFileResponse>>
    ) {
        if (screenShotPathList.isNullOrEmpty()) {
            callback.onSuccess(arrayListOf())
            return
        }
        println("需要上传${screenShotPathList.size}个")
        val uploadResultList = arrayListOf<VivoUploadFileResponse>()
        screenShotPathList.map {
            uploadScreenShot(packageName, it, { error -> callback.onFail(error) }) { uploadFile ->
                uploadResultList.add(uploadFile)
                if (uploadResultList.size == screenShotPathList.size) {
                    callback.onSuccess(uploadResultList)
                } else {
                    println("当前:${it}上传成功， 还有${screenShotPathList.size - uploadResultList.size}个")
                }
            }
        }
    }

    private fun uploadScreenShot(
        packageName: String,
        screenShotPath: String,
        onFail: ((String) -> Unit)? = null,
        callback: (VivoUploadFileResponse) -> Unit
    ) {
        uploadScreenShot(packageName, screenShotPath, object : SuccessCallback<VivoUploadFileResponse> {
            override fun onSuccess(t: VivoUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private val uploadScreenShotMap = mutableMapOf<String, VivoUploadFileResponse>()
    private fun uploadScreenShot(
        packageName: String, screenShotPath: String, callback: SuccessCallback<VivoUploadFileResponse>
    ) {
        val successResult = uploadScreenShotMap[packageName + screenShotPath]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
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
                val response = gson.fromJson(t, VivoUploadFileResponse::class.java)
                if (response.isSuccess()) {
                    uploadScreenShotMap[packageName + screenShotPath] = response
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

    private fun uploadApkList(
        uploadData: UploadData, onFail: ((String) -> Unit)? = null, callback: (List<VivoUploadFileResponse>) -> Unit
    ) {
        uploadApkList(uploadData, object : SuccessCallback<List<VivoUploadFileResponse>> {
            override fun onSuccess(t: List<VivoUploadFileResponse>) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadApkList(uploadData: UploadData, callback: SuccessCallback<List<VivoUploadFileResponse>>) {
        //先上传apk和相应的图片资源
        val uploadList = uploadData.apk.abiApk.filter {
            uploadAbi().contains(it.abi)
        }
        if (uploadList.size != uploadAbi().size) {
            println("缺少相关Apk,不能上传,请检查，当前符合包：${uploadList.map { it.abi.type }}")
            return
        }
        val uploadResultList = arrayListOf<VivoUploadFileResponse>()
        println("上传Apk中，需要上传:${uploadList.map { it.abi.type }} ${uploadList.size}个Apk")
        uploadList.forEach {
            uploadApk(it, { error -> callback.onFail(error) }) { uploadFile ->
                uploadResultList.add(uploadFile)
                println("第${uploadResultList.size}个Apk上传完成")
                if (uploadResultList.size == uploadList.size) {
                    println("Apk全部上传完成")
                    callback.onSuccess(uploadResultList)
                }
            }
        }
    }

    private fun uploadApk(apk: Apk, onFail: ((String) -> Unit)? = null, callback: (VivoUploadFileResponse) -> Unit) {
        uploadApk(apk, object : SuccessCallback<VivoUploadFileResponse> {
            override fun onSuccess(t: VivoUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadApkMap = mutableMapOf<Apk, VivoUploadFileResponse>()
    private fun uploadApk(apk: Apk, callback: SuccessCallback<VivoUploadFileResponse>) {
        val successResult = uploadApkMap[apk]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
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
                    println("文件:${apk.fileName} 上传进度:${curProcess}%")
                }
            }
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, VivoUploadFileResponse::class.java)
                if (response.isSuccess()) {
                    response.abi = apk.abi
                    uploadApkMap[apk] = response
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

    @Transient
    private val methodScreenShot = "app.upload.screenshot"

    @Transient
    private val methodApkAll = "app.upload.apk.app"

    @Transient
    private val methodApk32 = "app.upload.apk.app.32"

    @Transient
    private val methodApk64 = "app.upload.apk.app.64"

    @Transient
    private val methodUpdate = "app.sync.update.subpackage.app"

    @Transient
    private val methodQuery = "app.query.details"

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
        serialnumber32: String,
        serialnumber64: String,
        updateDesc: String,
        screenshot: String? = null
    ) = mutableMapOf(
        Pair("packageName", packageName),
        Pair("versionCode", versionCode),
        Pair("apk32", serialnumber32),
        Pair("apk64", serialnumber64),
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
internal data class VivoUploadFileResponse(
    @kotlinx.serialization.Transient var abi: AbiType? = null
) : VivoBaseResponse<VivoFileData>()

@Serializable
internal open class VivoBaseResponse<T>(
    val code: Int? = null,
    val subCode: String? = null,
    val msg: String? = null,
    val data: T? = null,
) {
    fun isSuccess() = subCode == "0"
}

@Serializable
internal data class VivoFileData(
    val packageName: String? = null,
    val serialnumber: String? = null,
    val versionCode: String? = null,
    val versionName: String? = null,
    val fileMd5: String? = null,
)