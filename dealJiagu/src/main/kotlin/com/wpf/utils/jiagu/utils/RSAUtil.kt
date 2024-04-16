package com.wpf.utils.jiagu.utils

import com.wpf.utils.jiagu.utils.RSAUtil.decryptByPublicKey
import com.wpf.utils.jiagu.utils.RSAUtil.encryptByPrivateKey
import com.wpf.utils.jiagu.utils.RSAUtil.generateKeyPair
import org.apache.commons.codec.binary.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


class RsaKeyPair(
    val publicKey: String,
    val privateKey: String,
)


object RSAUtil {
    /**
     * RSA编码
     */
    const val ALGORITHM: String = "RSA"

    /**
     * RSA最大加密明文大小
     */
    const val MAX_ENCRYPT_BLOCK: Int = 245

    /**
     * RSA最大解密密文大小
     */
    const val MAX_DECRYPT_BLOCK: Int = 256

    /**
     * 构建RSA密钥对
     * @return
     * @throws NoSuchAlgorithmException
     */
    @Throws(NoSuchAlgorithmException::class)
    fun generateKeyPair(): RsaKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)

        // 不加入默认种子, 每次生成的密钥对会变化
        keyPairGenerator.initialize(2048, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        val rsaPublicKey = keyPair.public as RSAPublicKey
        val rsaPrivateKey = keyPair.private as RSAPrivateKey
        val publicKeyString: String = Base64.encodeBase64String(rsaPublicKey.encoded)
        val privateKeyString: String = Base64.encodeBase64String(rsaPrivateKey.encoded)
        return RsaKeyPair(publicKeyString, privateKeyString)
    }

    /**
     * 私钥加密
     *
     * @param data              加密数据
     * @param privateKeyText    私钥
     * @return                  密文
     * @throws Exception        加密过程中的异常信息
     */
    @Throws(Exception::class)
    fun encryptByPrivateKey(data: String, privateKeyText: String): String {
        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(Base64.decodeBase64(privateKeyText))
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        val privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, privateKey)
        val inputLen = data.toByteArray().size
        val out = ByteArrayOutputStream()
        var offset = 0
        var cache: ByteArray
        var i = 0
        // 对数据分段加密
        while (inputLen - offset > 0) {
            cache = if (inputLen - offset > MAX_ENCRYPT_BLOCK) {
                cipher.doFinal(data.toByteArray(), offset, MAX_ENCRYPT_BLOCK)
            } else {
                cipher.doFinal(data.toByteArray(), offset, inputLen - offset)
            }
            out.write(cache, 0, cache.size)
            i++
            offset = i * MAX_ENCRYPT_BLOCK
        }
        val encryptedData = out.toByteArray()
        out.close()
        // 获取加密内容使用base64进行编码,并以UTF-8为标准转化成字符串
        // 加密后的字符串
        return Base64.encodeBase64String(encryptedData)
    }

    /**
     * 公钥解密
     *
     * @param data          加密字符串
     * @param publicKeyText 公钥
     * @return              明文
     * @throws Exception    解密过程中的异常信息
     */
    @Throws(Exception::class)
    fun decryptByPublicKey(data: String, publicKeyText: String): String {
        val x509EncodedKeySpec = X509EncodedKeySpec(Base64.decodeBase64(publicKeyText))
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        val publicKey = keyFactory.generatePublic(x509EncodedKeySpec)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, publicKey)
        val dataBytes = Base64.decodeBase64(data)
        val inputLen = dataBytes.size
        val out = ByteArrayOutputStream()
        var offset = 0
        var cache: ByteArray
        var i = 0
        // 对数据分段解密
        while (inputLen - offset > 0) {
            cache = if (inputLen - offset > MAX_DECRYPT_BLOCK) {
                cipher.doFinal(dataBytes, offset, MAX_DECRYPT_BLOCK)
            } else {
                cipher.doFinal(dataBytes, offset, inputLen - offset)
            }
            out.write(cache, 0, cache.size)
            i++
            offset = i * MAX_DECRYPT_BLOCK
        }
        val decryptedData = out.toByteArray()
        out.close()
        // 解密后的内容
        return String(decryptedData, StandardCharsets.UTF_8)
    }
}

fun main() {
    try {
        // 生成密钥对
        val rsaKeyPair = generateKeyPair()
        println("私钥:" + rsaKeyPair.privateKey)
        println("公钥:" + rsaKeyPair.publicKey)

        // RSA 私钥加密
        val data1 = "待加密的文字内容"
        val encryptPrivateKeyData = encryptByPrivateKey(data1, rsaKeyPair.privateKey)
        println("私钥加密后内容:$encryptPrivateKeyData")
        // RSA 公钥解密
        val decryptPublicKeyData = decryptByPublicKey(encryptPrivateKeyData, rsaKeyPair.publicKey)
        println("公钥解密后内容:$decryptPublicKeyData")

    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        print("加解密异常")
    }
}