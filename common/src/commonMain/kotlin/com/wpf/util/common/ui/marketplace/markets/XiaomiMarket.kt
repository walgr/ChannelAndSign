package com.wpf.util.common.ui.marketplace.markets

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.http.Http
import com.wpf.util.common.ui.marketplace.UploadData
import com.wpf.util.common.ui.utils.Callback
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.*
import java.security.PublicKey
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Cipher


object XiaomiMarket : Market {

    var userName: String = ""
    var password: String = ""
    var pubKeyPath: String = ""         //小米公钥路径

    override val baseUrl: String = "http://api.developer.xiaomi.com/devupload"
    override fun uploadAbi() = arrayOf(AbiType.Abi32, AbiType.Abi64)

    override fun push(uploadData: UploadData) {
        if (uploadData.apk.abiApk.isEmpty()) return
        val api = "/dev/push"
        Http.post(baseUrl + api, request = {
            setBody(MultiPartFormDataContent(
                formData {
                    val requestDataJson = gson.toJson(
                        XiaomiPushData(
                            userName = userName,
                            appInfo = gson.toJson(
                                XiaomiApk(
                                    appName = uploadData.apk.abiApk.getOrNull(0)?.appName ?: "",
                                    packageName = uploadData.apk.abiApk.getOrNull(0)?.packageName ?: "",
                                    updateDesc = uploadData.description,
                                )
                            ),
                            apk = File(uploadData.apk.abiApk.getOrNull(0)?.filePath ?: ""),
                            secondApk = File(uploadData.apk.abiApk.getOrNull(1)?.filePath ?: ""),
                        )
                    )
                    append("RequestData", requestDataJson)
                    append("SIG", encryptByPublicKey(gson.toJson(
                        JsonObject().apply {
                            addProperty("sig", gson.toJson(JsonArray().apply {
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
                                        addProperty("name", "screenshot_${index+1}")
                                        addProperty("hash", getFileMD5(imagePath))
                                    })
                                }
                            }))
                            addProperty("password", password)
                        }
                    ), pubKey))
                }
            ))
        }, callback = object : Callback<String> {
            override fun onSuccess(t: String) {

            }

            override fun onFail(msg: String) {

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

    private const val KEY_SIZE = 1024
    private const val GROUP_SIZE = KEY_SIZE / 8
    private const val ENCRYPT_GROUP_SIZE = GROUP_SIZE - 11
    private const val KEY_ALGORITHM = "RSA/NONE/PKCS1Padding"
    private val gson = Gson()
    private var pubKey: PublicKey? = null

    /**
     * 公钥加密
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun encryptByPublicKey(str: String, publicKey: PublicKey?): String {
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
        return try {
            (CertificateFactory.getInstance("X.509").generateCertificate(x509Is) as X509Certificate).publicKey
        } finally {
            try {
                x509Is.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // 加载BC库
    init {
        Security.addProvider(BouncyCastleProvider());
        try {
            pubKey = getPublicKeyByX509Cer(pubKeyPath);
        } catch (ignore: Exception) {
        }
    }
}