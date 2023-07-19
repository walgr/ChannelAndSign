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
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.widget.common.InputView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.*
import java.security.PublicKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Cipher

class XiaomiMarket : Market {

    var userName: String = ""
    var password: String = ""
    var pubKeyPath: String = ""         //小米公钥路径
        set(value) {
            field = value
            initPubkey()
        }

    override var isSelect = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    override val name: String = "Xiaomi"

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
                    modifier = Modifier.weight(1f), input = xiaomiAccountPub, hint = "请配置小米Pubkey文件路径"
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

    override fun query(uploadData: UploadData) {
        super.query(uploadData)
        if (uploadData.apk.abiApk.isEmpty()) return
        val api = "/dev/query"
        Http.submitForm(
            baseUrl + api, parameters {
                val requestDataJson = JsonObject().apply {
                    addProperty("packageName", uploadData.packageName()!!)
                    addProperty("userName", userName)
                }.toString()
                append("RequestData", requestDataJson)
                val sigJson = JsonObject().apply {
                    addProperty("password", password)
                    add(
                        "sig", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("name", "RequestData")
                                addProperty("hash", DigestUtils.md5Hex(requestDataJson))
                            })
                        }
                    )
                }.toString()
                append("SIG", encryptByPublicKey(sigJson, pubKey))
            }, {
                timeout {
                    socketTimeoutMillis = 30000
                }
            }, callback =
            object : Callback<String> {
                override fun onSuccess(t: String) {
                    println("小米接口请求成功,结果:$t")
                }

                override fun onFail(msg: String) {
                    println("小米接口请求失败,结果:$msg")
                }
            })
    }

    override fun push(uploadData: UploadData) {
        if (uploadData.apk.abiApk.isEmpty()) return
        val api = "/dev/push"
        Http.post(baseUrl + api, request = {
            setBody(MultiPartFormDataContent(formData {
                val xiaomiPushData = XiaomiPushData(
                    userName = userName,
                    appInfo = gson.toJson(
                        XiaomiApk(
                            appName = uploadData.apk.abiApk.getOrNull(0)?.appName ?: "",
                            packageName = uploadData.apk.abiApk.getOrNull(0)?.packageName ?: "",
                            updateDesc = uploadData.description,
                        )
                    ),
                    apk = File(uploadData.apk.abiApk.getOrNull(0)?.filePath ?: ""),
                    secondApk = if (uploadData.apk.abiApk.getOrNull(1) == null) null else File(
                        uploadData.apk.abiApk.getOrNull(
                            1
                        )?.filePath ?: ""
                    ),
                )
                val requestDataJson = gson.toJson(xiaomiPushData)
                append("apk", xiaomiPushData.apk.readBytes())
                xiaomiPushData.secondApk?.let {
                    append("secondApk", xiaomiPushData.secondApk.readBytes())
                }
                xiaomiPushData.icon?.let {
                    append("icon", xiaomiPushData.icon.readBytes())
                }
                xiaomiPushData.screenshot_1?.let {
                    append("screenshot_1", xiaomiPushData.screenshot_1.readBytes())
                }
                xiaomiPushData.screenshot_2?.let {
                    append("screenshot_2", xiaomiPushData.screenshot_2.readBytes())
                }
                xiaomiPushData.screenshot_3?.let {
                    append("screenshot_3", xiaomiPushData.screenshot_3.readBytes())
                }
                xiaomiPushData.screenshot_4?.let {
                    append("screenshot_4", xiaomiPushData.screenshot_4.readBytes())
                }
                xiaomiPushData.screenshot_5?.let {
                    append("screenshot_5", xiaomiPushData.screenshot_5.readBytes())
                }
                append("RequestData", requestDataJson)
                append("SIG", encryptByPublicKey(JsonObject().apply {
                    add("sig", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("name", "RequestData")
                            addProperty("hash", DigestUtils.md5Hex(requestDataJson))
                        })
                        uploadData.apk.abiApk.forEach {
                            add(JsonObject().apply {
                                addProperty("name", "apk")
                                addProperty("hash", getFileMD5(it.filePath))
                            })
                        }
                        uploadData.apk.abiApk.getOrNull(0)?.appIcon?.let {
                            add(JsonObject().apply {
                                addProperty("name", "icon")
                                addProperty("hash", getFileMD5(it))
                            })
                        }
                        uploadData.imageList?.forEachIndexed { index, imagePath ->
                            add(JsonObject().apply {
                                addProperty("name", "screenshot_${index + 1}")
                                addProperty("hash", getFileMD5(imagePath))
                            })
                        }
                    })
                    addProperty("password", password)
                }.toString(), pubKey))
            }))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {
                println("小米接口请求成功,结果:$t")
            }

            override fun onFail(msg: String) {
                println("小米接口请求失败,结果:$msg")
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
        initPubkey()
    }

    fun initPubkey() {
        if (pubKeyPath.isNotEmpty()) {
            pubKey = getPublicKeyByX509Cer(pubKeyPath)
        }
    }
}