package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
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
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

data class SamsungMarket(
    var contentId: String = "",
    var serviceAccountId: String = "",
    var privateKeyPath: String = ""
) : Market {

    override val name: String = MarketType.三星.channelName

    @Transient
    override val baseUrl: String = "http://devapi.samsungapps.com"

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override var isSelect: Boolean = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is SamsungMarket) return
        val contentId = remember { mutableStateOf(market.contentId) }
        contentId.value = market.contentId
        val serviceAccountId = remember { mutableStateOf(market.serviceAccountId) }
        serviceAccountId.value = market.serviceAccountId
        val privateKeyPath = remember { mutableStateOf(market.privateKeyPath) }
        privateKeyPath.value = market.privateKeyPath

        Column {
            InputView(input = contentId, hint = "请配置三星内容ID") {
                contentId.value = it
                market.contentId = it
            }
            InputView(input = serviceAccountId, hint = "请配置三星服务账号ID") {
                serviceAccountId.value = it
                market.serviceAccountId = it
            }
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                InputView(
                    modifier = Modifier.weight(1f), input = privateKeyPath, hint = "请配置三星私钥文件路径"
                ) {
                    privateKeyPath.value = it
                }
                Button(onClick = {
                    FileSelector.showFileSelector(arrayOf("cer")) {
                        privateKeyPath.value = it
                        market.privateKeyPath = it
                    }
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("选择")
                }
            }
        }
    }

    override fun query(uploadData: UploadData, callback: Callback<MarketType>) {
        super.query(uploadData, callback)
        if (uploadData.packageName().isNullOrEmpty()) return
        getAppInfo(uploadData.packageName()!!, object : SuccessCallback<SamsungAppInfo> {
            override fun onSuccess(t: SamsungAppInfo) {
                callback.onSuccess(MarketType.三星)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                callback.onFail(msg)
            }
        })
    }

    private fun getAppInfo(
        packageName: String,
        onFail: ((String) -> Unit)? = null, callback: (SamsungAppInfo) -> Unit
    ) {
        getAppInfo(packageName, object : SuccessCallback<SamsungAppInfo> {
            override fun onSuccess(t: SamsungAppInfo) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun getAppInfo(packageName: String, callback: SuccessCallback<SamsungAppInfo>) {
        getToken(packageName) {
            Http.get("$baseUrl/seller/contentInfo?contentId=${contentId}", {
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append("service-account-id", serviceAccountId)
                    bearerAuth(it)
                }
            }, object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response =
                        gson.fromJson<List<SamsungAppInfo>>(t, object : TypeToken<List<SamsungAppInfo>>() {}.type)
                    if (response.isNotEmpty()) {
                        callback.onSuccess(response[0])
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
        update(uploadData, { callback.onFail(it) }) {
            submit(uploadData.packageName()!!, callback = object : SuccessCallback<String> {
                override fun onSuccess(t: String) {
                    callback.onSuccess(MarketType.三星)
                }

                override fun onFail(msg: String) {
                    super.onFail(msg)
                    println(msg)
                    callback.onFail("$name:$msg")
                }

            })
        }
    }

    private fun submit(packageName: String, callback: SuccessCallback<String>) {
        getToken(packageName, { callback.onFail(it) }) { token ->
            Http.post("$baseUrl/seller/contentSubmit", {
                timeout {
                    requestTimeoutMillis = 10000
                }
                headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append("service-account-id", serviceAccountId)
                    bearerAuth(token)
                }
                setBody(mapOf("contentId" to contentId))
            }, object : Callback<String> {
                override fun onSuccess(t: String) {
                    callback.onSuccess("")
                }

                override fun onFail(msg: String) {
                    callback.onFail(msg)
                }

            })
        }
    }

    @Transient
    private var binaryList = mutableListOf<SamsungApkBody>()

    @Transient
    private var screenShotList = mutableListOf<SamsungScreenShotBody>()
    private fun <T> addNewToList(list: MutableList<T>, samsungApkBodyList: List<T>): List<T> {
        if (samsungApkBodyList.isEmpty()) return list
        if (list.size + samsungApkBodyList.size > 10) {
            list.removeAll(list.subList(0, list.size + samsungApkBodyList.size - 10))
        }
        list.addAll(samsungApkBodyList)
        return list
    }

    private fun update(
        uploadData: UploadData, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit
    ) {
        update(uploadData, object : SuccessCallback<String> {
            override fun onSuccess(t: String) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun update(uploadData: UploadData, callback: SuccessCallback<String>) {
        getToken(uploadData.packageName()!!, onFail = { callback.onFail(it) }) { token ->
            getAppInfo(uploadData.packageName()!!, { callback.onFail(it) }) {
                binaryList = it.binaryList?.toMutableList() ?: arrayListOf()
                screenShotList = it.screenshots?.toMutableList() ?: arrayListOf()
                updateState(uploadData.packageName()!!) {
                    uploadScreenShotList(
                        uploadData.packageName()!!,
                        uploadData.imageList,
                        { error -> callback.onFail(error) }) { screensResponse ->
                        uploadApkList(uploadData, { error -> callback.onFail(error) }) { apksResponse ->
                            Http.submitForm("$baseUrl/seller/contentUpdate", parameters {
                                append("contentId", contentId)
                                append("newFeature", uploadData.description)
                                if (screensResponse.isNotEmpty()) {
                                    append("screenshots", gson.toJson(addNewToList(screenShotList, screenShotList)))
                                }
                                append(
                                    "binaryList",
                                    gson.toJson(addNewToList(binaryList, apksResponse.map { uploadFile ->
                                        SamsungApkBody(
                                            filekey = uploadFile.fileKey!!,
                                            fileName = uploadFile.apk?.fileName!!,
                                            packageName = uploadFile.apk?.packageName!!,
                                            versionCode = uploadFile.apk?.versionCode ?: "",
                                            versionName = uploadFile.apk?.versionName ?: ""
                                        )
                                    }))
                                )
                            }, {
                                headers {
                                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                                    append("service-account-id", serviceAccountId)
                                    bearerAuth(token)
                                }
                            }, object : Callback<String> {
                                override fun onSuccess(t: String) {
                                    val response = gson.fromJson(t, SamSungBaseResponse::class.java)
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
            }
        }
    }

    private fun uploadApkList(
        uploadData: UploadData, onFail: ((String) -> Unit)? = null, callback: (List<SamsungUploadFileResponse>) -> Unit
    ) {
        uploadApkList(uploadData, object : SuccessCallback<List<SamsungUploadFileResponse>> {
            override fun onSuccess(t: List<SamsungUploadFileResponse>) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadApkList(uploadData: UploadData, callback: SuccessCallback<List<SamsungUploadFileResponse>>) {
        val uploadApkList = uploadData.apk.abiApk.filter {
            uploadAbi().contains(it.abi)
        }
        if (uploadApkList.isEmpty()) {
            callback.onFail("未找到需要上传的apk")
            return
        }
        val returnResponse = mutableListOf<SamsungUploadFileResponse>()
        uploadApkList.forEach { apk ->
            uploadApk(apk, { msg -> callback.onFail("$apk:$msg") }) {
                returnResponse.add(it)
                if (returnResponse.size == uploadApkList.size) {
                    callback.onSuccess(returnResponse)
                }
            }
        }
    }

    private fun uploadApk(
        apk: Apk, onFail: ((String) -> Unit)? = null, callback: (SamsungUploadFileResponse) -> Unit
    ) {
        uploadApk(apk, object : SuccessCallback<SamsungUploadFileResponse> {
            override fun onSuccess(t: SamsungUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }
    @delegate:Transient
    private val uploadApkMap by autoSaveMap("SamsungUploadApkMap") { mutableMapOf<String, SamsungUploadFileResponse>() }
    private fun uploadApk(apk: Apk, callback: SuccessCallback<SamsungUploadFileResponse>) {
        val successResult = uploadApkMap[apk.filePath]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        createUploadFileId(apk.packageName, apk.filePath, onFail = {
            callback.onFail(it)
        }) { fileId ->
            getToken(apk.packageName, onFail = {
                callback.onFail(it)
            }) {
                println("文件:${apk.fileName} 上传地址:${fileId.url}")
                Http.post(fileId.url!!.replace("https", "http"), {
                    timeout {
                        requestTimeoutMillis = 600000
                    }
                    headers {
                        append(HttpHeaders.ContentType, ContentType.MultiPart.FormData)
                        append("service-account-id", serviceAccountId)
                        bearerAuth(it)
                    }
                    setBody(MultiPartFormDataContent(formData {
                        append("\"sessionId\"", fileId.sessionId!!)
                        append("\"file\"", File(apk.filePath).readBytes(), apkHeader(apk.filePath))
                    }))
                    var lastProcess = 0L
                    onUpload { bytesSentTotal, contentLength ->
                        val curProcess = bytesSentTotal * 100 / contentLength
                        if (curProcess != lastProcess) {
                            lastProcess = curProcess
                            println("文件:${apk.fileName} 上传进度:${curProcess}%")
                        }
                    }
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, SamsungUploadFileResponse::class.java)
                        if (response.fileKey?.isEmpty() == false) {
                            response.apk = apk
                            uploadApkMap[apk.filePath] = response
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

    private fun uploadScreenShotList(
        packageName: String,
        screenShotPathList: List<String>? = null,
        onFail: ((String) -> Unit)? = null,
        callback: (List<SamsungUploadFileResponse>) -> Unit
    ) {
        uploadScreenShotList(
            packageName,
            screenShotPathList,
            callback = object : SuccessCallback<List<SamsungUploadFileResponse>> {
                override fun onSuccess(t: List<SamsungUploadFileResponse>) {
                    callback.invoke(t)
                }

                override fun onFail(msg: String) {
                    super.onFail(msg)
                    onFail?.invoke(msg)
                }
            })
    }

    private fun uploadScreenShotList(
        packageName: String,
        screenShotPathList: List<String>? = null,
        returnResponseList: MutableList<SamsungUploadFileResponse>? = null,
        callback: SuccessCallback<List<SamsungUploadFileResponse>>
    ) {
        if (screenShotPathList.isNullOrEmpty()) {
            callback.onSuccess(arrayListOf())
            return
        }
        val returnResponse = returnResponseList ?: mutableListOf()
        val screenShotPath = screenShotPathList[returnResponse.size]
        uploadScreenShot(packageName, screenShotPath, { msg -> callback.onFail("$screenShotPath:$msg") }) {
            returnResponse.add(it)
            if (returnResponse.size == screenShotPathList.size) {
                callback.onSuccess(returnResponse)
            } else {
                uploadScreenShotList(packageName, screenShotPathList, returnResponse, callback)
            }
        }
    }

    private fun uploadScreenShot(
        packageName: String,
        screenShotPath: String, onFail: ((String) -> Unit)? = null, callback: (SamsungUploadFileResponse) -> Unit
    ) {
        uploadScreenShot(packageName, screenShotPath, object : SuccessCallback<SamsungUploadFileResponse> {
            override fun onSuccess(t: SamsungUploadFileResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    private fun uploadScreenShot(
        packageName: String,
        screenShotPath: String,
        callback: SuccessCallback<SamsungUploadFileResponse>
    ) {
        createUploadFileId(packageName, screenShotPath, onFail = { callback.onFail(it) }) { fileId ->
            getToken(packageName, onFail = { callback.onFail(it) }) {
                Http.post(fileId.url!!, {
                    timeout {
                        requestTimeoutMillis = 120000
                    }
                    headers {
                        append(HttpHeaders.ContentType, ContentType.MultiPart.FormData)
                        append("service-account-id", serviceAccountId)
                        bearerAuth(it)
                    }
                    setBody(MultiPartFormDataContent(formData {
                        append("\"sessionId\"", fileId.sessionId!!)
                        append("\"file\"", File(screenShotPath).readBytes(), pngHeader(screenShotPath))
                    }))
                }, object : Callback<String> {
                    override fun onSuccess(t: String) {
                        val response = gson.fromJson(t, SamsungUploadFileResponse::class.java)
                        if (response.fileKey?.isEmpty() == false) {
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

    private fun createUploadFileId(
        packageName: String,
        key: String,
        onFail: ((String) -> Unit)? = null,
        callback: (SamsungCreateFileIdResponse) -> Unit
    ) {
        createUploadFileId(packageName, key, object : SuccessCallback<SamsungCreateFileIdResponse> {
            override fun onSuccess(t: SamsungCreateFileIdResponse) {
                callback.invoke(t)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
            }
        })
    }

    @Transient
    private val uploadFileIdMap =  mutableMapOf<String, SamsungCreateFileIdResponse>()

    private fun createUploadFileId(
        packageName: String,
        key: String,
        callback: SuccessCallback<SamsungCreateFileIdResponse>
    ) {
        val successResult = uploadFileIdMap[key]
        if (successResult != null) {
            callback.onSuccess(successResult)
            return
        }
        getToken(packageName) {
            Http.post("$baseUrl/seller/createUploadSessionId", {
                timeout {
                    requestTimeoutMillis = 30000
                }
                headers {
                    append("service-account-id", serviceAccountId)
                    bearerAuth(it)
                }
            }, callback = object : Callback<String> {
                override fun onSuccess(t: String) {
                    val response = gson.fromJson(t, SamsungCreateFileIdResponse::class.java)
                    if (response?.sessionId?.isEmpty() == false && response.url?.isEmpty() == false) {
                        uploadFileIdMap[key] = response
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

    private fun updateState(packageName: String, onFail: ((String) -> Unit)? = null, callback: (String) -> Unit) {
        callback.invoke("")
//        updateState(packageName, object : SuccessCallback<String> {
//            override fun onSuccess(t: String) {
//                callback.invoke(t)
//            }
//
//            override fun onFail(msg: String) {
//                super.onFail(msg)
//                onFail?.invoke(msg)
//            }
//        })
    }

    private fun updateState(packageName: String, callback: SuccessCallback<String>) {
        getToken(packageName) {
            Http.submitForm("$baseUrl/seller/contentStatusUpdate", parameters {
                append("contentId", contentId)
                append("contentStatus", "FOR_SALE")
            }, {
                timeout {
                    requestTimeoutMillis = 10000
                }
                io.ktor.http.headers {
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    append("service-account-id", serviceAccountId)
                    bearerAuth(it)
                }
            }, callback = object : Callback<String> {
                override fun onSuccess(t: String) {
                    callback.onSuccess("")
                }

                override fun onFail(msg: String) {
                    callback.onFail(msg)
                }

            })
        }
    }

    //过期时间
    @Transient
    private val extTime = 20 * 60

    @Transient
    private var token = autoSaveMap("samsungToken") { mutableMapOf<String, MutableMap<Long, String>>() }

    private fun getEfficientToken(packageName: String): String {
        var efficientToken = ""
        val noEfficientToken = mutableListOf<Long>()
        token[packageName].forEach { (t, u) ->
            if ((System.currentTimeMillis() / 1000) - t < extTime) {
//                efficientToken = u
            } else {
                noEfficientToken.add(t)
            }
        }
        noEfficientToken.forEach {
            token[packageName].remove(it)
        }
        return efficientToken
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
        Http.post("$baseUrl/auth/accessToken", {
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json)
                bearerAuth(initJWT())
            }
        }, object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, SamSungTokenResponse::class.java)
                if (response.isSuccess() && !response.createdItem?.accessToken.isNullOrEmpty()) {
                    token[packageName][System.currentTimeMillis() / 1000] = response.createdItem?.accessToken!!
//                    token.saveData()
                    callback.onSuccess(response.createdItem.accessToken)
                } else {
                    callback.onFail("")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }

        })
    }

    private fun checkToken(token: String, onFail: ((String) -> Unit)? = null, callback: (Boolean) -> Unit) {
        checkToken(token, object : SuccessCallback<Unit> {
            override fun onSuccess(t: Unit) {
                callback.invoke(true)
            }

            override fun onFail(msg: String) {
                super.onFail(msg)
                onFail?.invoke(msg)
                callback.invoke(false)
            }
        })
    }
    private fun checkToken(token: String, callback: SuccessCallback<Unit>) {
        Http.get("$baseUrl/auth/checkAccessToken", {
            headers {
                append("service-account-id", serviceAccountId)
                bearerAuth(token)
            }
        }, object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, SamSungBaseResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess(Unit)
                } else {
                    callback.onFail("")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }

        })
    }

    private fun revokeToken(packageName: String, callback: SuccessCallback<Unit>) {
        val efficientToken = getEfficientToken(packageName)
        if (efficientToken.isEmpty()) {
            callback.onSuccess(Unit)
            return
        }
        Http.delete("$baseUrl/auth/revokeAccessToken", {
            headers {
                append("service-account-id", serviceAccountId)
                bearerAuth(efficientToken)
            }
        }, object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, SamSungBaseResponse::class.java)
                if (response.isSuccess()) {
                    var findKey = 0L
                    token[packageName].forEach { (t, u) ->
                        if (u == efficientToken) {
                            findKey = t
                        }
                    }
                    token[packageName].remove(findKey)
                    callback.onSuccess(Unit)
                } else {
                    callback.onFail("")
                }
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }

        })
    }

    private fun initJWT(): String {
        val curTime = System.currentTimeMillis() / 1000
        return JWT.create().withClaim("iss", serviceAccountId).withArrayClaim("scopes", arrayOf("publishing"))
            .withClaim("iat", curTime).withClaim("exp", curTime + extTime).sign(
                Algorithm.RSA256(getPriKeyByX509Cer(privateKeyPath) as RSAPrivateKey)
            )
    }

    private fun getPubKeyByX509Cer(cerFilePath: String): PublicKey? {
        val keyIs: InputStream = FileInputStream(cerFilePath)
        try {
            return KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(Base64.getMimeDecoder().decode(keyIs.readBytes())))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching {
                keyIs.close()
            }
        }
        return null
    }

    private fun getPriKeyByX509Cer(cerFilePath: String): PrivateKey? {
        val keyIs: InputStream = FileInputStream(cerFilePath)
        try {
            return KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(keyIs.readBytes())))
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                keyIs.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    override fun clearCache() {
        super.clearCache()
        token.clear()
    }
}

@Serializable
open class SamSungBaseResponse(
    private val ok: Boolean? = null,
    private val httpStatus: String? = null,
) {
    fun isSuccess(): Boolean {
        return ok == true || "OK" == httpStatus
    }
}

@Serializable
data class SamsungContentResponse(
    val contentId: String? = null,
    val contentName: String? = null,
    val contentStatus: String? = null,
    val standardPrice: String? = null,
    val paid: String? = null,
    val modifyDate: String? = null,
)

@Serializable
data class SamsungUploadFileResponse(
    val fileKey: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null,
    val errorCode: String? = null,
    val errorMsg: String? = null,

    @kotlinx.serialization.Transient  @Transient var apk: Apk? = null
)

@Serializable
data class SamsungCreateFileIdResponse(
    val url: String? = null,
    val sessionId: String? = null,
)

@Serializable
internal data class SamSungTokenResponse(
    val createdItem: SamSungToken? = null
) : SamSungBaseResponse()

@Serializable
internal data class SamSungToken(
    val accessToken: String? = null
)

@Serializable
internal data class SamsungAppInfo(
    val contentId: String? = null,
    val appTitle: String? = null,
    val binaryList: List<SamsungApkBody>? = null,
    val screenshots: List<SamsungScreenShotBody>? = null,
)

@Serializable
internal data class SamsungApkBody(
    val filekey: String,
    val fileName: String,
    val packageName: String,
    val versionCode: String,
    val versionName: String,
    val nativePlatforms: String? = null,
    val apiminSdkVersion: String? = null,
    val apimaxSdkVersion: String? = null,
    val iapSdk: String? = "N",
    val gms: String? = "N",
    val binarySeq: String? = "1",
)

@Serializable
internal data class SamsungScreenShotBody(
    val screenshotPath: String,
    val screenshotKey: String,
    val reuseYn: Boolean = false,
)