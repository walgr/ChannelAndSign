package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.markets.base.*
import com.wpf.util.common.ui.utils.Callback
import com.wpf.util.common.ui.utils.FileSelector
import com.wpf.util.common.ui.utils.asFile
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.*
import java.security.PublicKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Cipher

data class XiaomiMarket(
    var userName: String = "",
    var password: String = "",
) : Market {

    var pubKeyPath: String = ""         //小米公钥路径
        set(value) {
            field = value
            initPubkey()
        }

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = MarketType.小米.channelName

    @Transient
    override val baseUrl: String = "https://api.developer.xiaomi.com/devupload"
    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is XiaomiMarket) return
        val xiaomiAccount = remember { mutableStateOf(market.userName) }
        xiaomiAccount.value = market.userName
        val xiaomiAccountPassword = remember { mutableStateOf(market.password) }
        xiaomiAccountPassword.value = market.password
        val xiaomiAccountPub = remember { mutableStateOf(market.pubKeyPath) }
        xiaomiAccountPub.value = market.pubKeyPath

        Column {
            InputView(input = xiaomiAccount, hint = "请配置小米登录邮箱帐号") {
                xiaomiAccount.value = it
                market.userName = it
            }
            InputView(input = xiaomiAccountPassword, hint = "请配置小米账号密码") {
                xiaomiAccountPassword.value = it
                market.password = it
            }
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                InputView(
                    modifier = Modifier.weight(1f), input = xiaomiAccountPub, hint = "请配置小米公钥文件路径"
                ) {
                    xiaomiAccountPub.value = it
                }
                Button(onClick = {
                    FileSelector.showFileSelector(arrayOf("cer")) {
                        xiaomiAccountPub.value = it
                        market.pubKeyPath = it
                    }
                }, modifier = Modifier.padding(start = 8.dp)) {
                    Text("选择")
                }
            }
        }
    }

    override fun query(uploadData: UploadData, callback: Callback<MarketType>) {
        super.query(uploadData, callback)
        if (uploadData.apk.abiApk.isEmpty()) return
        val api = "/dev/query"
        Http.submitForm(baseUrl + api, parameters {
            val requestDataJson = JsonObject().apply {
                addProperty("packageName", uploadData.packageName()!!)
                addProperty("userName", userName)
            }.toString()
            append("RequestData", requestDataJson)
            val sigJson = JsonObject().apply {
                addProperty("password", password)
                add("sig", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("name", "RequestData")
                        addProperty("hash", DigestUtils.md5Hex(requestDataJson))
                    })
                })
            }.toString()
            append("SIG", encryptByPublicKey(sigJson, pubKey))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                callback.onSuccess(MarketType.小米)
            }

            override fun onFail(msg: String) {
                callback.onFail(msg)
            }
        })
    }

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.apk.abiApk.isEmpty()) return
        val api = "/dev/push"
        Http.post(baseUrl + api, request = {
            timeout {
                requestTimeoutMillis = 600000
            }
            setBody(MultiPartFormDataContent(formData {
                val xiaomiPushData = XiaomiPushData(
                    userName = userName,
                    appInfo = XiaomiApk(
                        appName = uploadData.apk.abiApk.getOrNull(0)?.appName ?: "",
                        packageName = uploadData.packageName()!!,
                        updateDesc = uploadData.description,
                    ),
                    apk = File(uploadData.apk.abiApk.getOrNull(0)?.filePath ?: ""),
                    secondApk = uploadData.apk.abiApk.getOrNull(1)?.filePath?.asFile(),
                    screenShot1 = uploadData.imageList?.getOrNull(0)?.asFile(),
                    screenShot2 = uploadData.imageList?.getOrNull(1)?.asFile(),
                    screenShot3 = uploadData.imageList?.getOrNull(2)?.asFile(),
                    screenShot4 = uploadData.imageList?.getOrNull(3)?.asFile(),
                    screenShot5 = uploadData.imageList?.getOrNull(4)?.asFile(),
                )
                val requestDataJson = gson.toJson(xiaomiPushData)
                append("RequestData", requestDataJson)
                append("SIG", encryptByPublicKey(JsonObject().apply {
                    add("sig", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("name", "RequestData")
                            addProperty("hash", DigestUtils.md5Hex(requestDataJson))
                        })
                        uploadData.apk.abiApk.getOrNull(0)?.let {
                            add(JsonObject().apply {
                                addProperty("name", "apk")
                                addProperty("hash", getFileMD5(it.filePath))
                            })
                        }
                        uploadData.apk.abiApk.getOrNull(1)?.let {
                            add(JsonObject().apply {
                                addProperty("name", "secondApk")
                                addProperty("hash", getFileMD5(it.filePath))
                            })
                        }
//                        uploadData.apk.abiApk.getOrNull(0)?.appIcon?.let {
//                            add(JsonObject().apply {
//                                addProperty("name", "icon")
//                                addProperty("hash", getFileMD5(it))
//                            })
//                        }
                        uploadData.imageList?.forEachIndexed { index, imagePath ->
                            add(JsonObject().apply {
                                addProperty("name", "screenshot_${index + 1}")
                                addProperty("hash", getFileMD5(imagePath))
                            })
                        }
                    })
                    addProperty("password", password)
                }.toString(), pubKey))
                append("apk", xiaomiPushData.apk.readBytes(), apkHeader(xiaomiPushData.apk.path))
                xiaomiPushData.secondApk?.let {
                    append("secondApk", xiaomiPushData.secondApk.readBytes(), apkHeader(xiaomiPushData.secondApk.path))
                }
//                xiaomiPushData.icon?.let {
//                    append("icon", xiaomiPushData.icon.readBytes())
//                }
                xiaomiPushData.screenShot1?.let {
                    append(
                        "screenshot_1",
                        xiaomiPushData.screenShot1.readBytes(),
                        pngHeader(xiaomiPushData.screenShot1.path)
                    )
                }
                xiaomiPushData.screenShot2?.let {
                    append(
                        "screenshot_2",
                        xiaomiPushData.screenShot2.readBytes(),
                        pngHeader(xiaomiPushData.screenShot2.path)
                    )
                }
                xiaomiPushData.screenShot3?.let {
                    append(
                        "screenshot_3",
                        xiaomiPushData.screenShot3.readBytes(),
                        pngHeader(xiaomiPushData.screenShot3.path)
                    )
                }
                xiaomiPushData.screenShot4?.let {
                    append(
                        "screenshot_4",
                        xiaomiPushData.screenShot4.readBytes(),
                        pngHeader(xiaomiPushData.screenShot4.path)
                    )
                }
                xiaomiPushData.screenShot5?.let {
                    append(
                        "screenshot_5",
                        xiaomiPushData.screenShot5.readBytes(),
                        pngHeader(xiaomiPushData.screenShot5.path)
                    )
                }
            }))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                val response = gson.fromJson(t, XiaomiBaseResponse::class.java)
                if (response.isSuccess()) {
                    callback.onSuccess(MarketType.小米)
                } else {
                    callback.onFail(name)
                }
            }

            override fun onFail(msg: String) {
                callback.onFail("$name:$msg")
            }
        })
    }

    @Throws(IOException::class)
    private fun getFileMD5(filePath: String): String? {
        var fis: FileInputStream? = null
        return try {
            fis = FileInputStream(filePath)
            DigestUtils.md5Hex(fis)
        } finally {
            fis?.close()
        }
    }

    @Transient
    private val KEY_SIZE = 1024

    @Transient
    private val GROUP_SIZE = KEY_SIZE / 8

    @Transient
    private val ENCRYPT_GROUP_SIZE = GROUP_SIZE - 11

    @Transient
    private val KEY_ALGORITHM = "RSA/NONE/PKCS1Padding"

    @Transient
    private var pubKey: PublicKey? = null

    /**
     * 公钥加密
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun encryptByPublicKey(str: String, publicKey: PublicKey?): String {
        val data = str.toByteArray()
        val out = ByteArrayOutputStream()
        val segment = ByteArray(ENCRYPT_GROUP_SIZE)
        var idx = 0
        val cipher = Cipher.getInstance(KEY_ALGORITHM, "BC")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        while (idx < data.size) {
            val remain = data.size - idx
            val segSize = if (remain > ENCRYPT_GROUP_SIZE) ENCRYPT_GROUP_SIZE else remain
            System.arraycopy(data, idx, segment, 0, segSize)
            out.write(cipher.doFinal(segment, 0, segSize))
            idx += segSize
        }
        return Hex.encodeHexString(out.toByteArray())
    }

    /**
     * 读取公钥
     *
     * @param cerFilePath
     * @return
     * @throws Exception
     */
    private fun getPublicKeyByX509Cer(cerFilePath: String): PublicKey? {
        val x509Is: InputStream = FileInputStream(cerFilePath)
        try {
            return (CertificateFactory.getInstance("X.509").generateCertificate(x509Is) as X509Certificate).publicKey
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                x509Is.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }

    // 加载BC库
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun initByData() {
        super.initByData()
        initPubkey()
    }

    private fun initPubkey() {
        if (pubKeyPath.isNotEmpty()) {
            pubKey = getPublicKeyByX509Cer(pubKeyPath)
        }
    }
}

@Serializable
internal data class XiaomiBaseResponse(
    val result: Int? = null,
    val message: String? = null,
) {
    fun isSuccess(): Boolean {
        return result == 0
    }
}