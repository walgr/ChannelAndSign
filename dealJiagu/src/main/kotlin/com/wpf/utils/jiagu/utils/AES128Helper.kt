package com.wpf.utils.jiagu.utils

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object AES128Helper {
    var DEFAULT_SECRET_KEY = "bajk3b4j3bvuoa3h"
    var KEY_VI = "mers46ha35ga23hn"

    private fun getEncryptCipher(model: Int): Cipher {
        val key: SecretKey = SecretKeySpec(DEFAULT_SECRET_KEY.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(model, key, IvParameterSpec(KEY_VI.toByteArray()))
        return cipher
    }

    fun encrypt(data: ByteArray, len: Int): ByteArray {
        val cipher = getEncryptCipher(Cipher.ENCRYPT_MODE)
        val decrypt = cipher.doFinal(data.copyOf(len))

        val temp = ByteArray(decrypt.size + data.size - len)
        System.arraycopy(decrypt, 0, temp, 0, decrypt.size)
        System.arraycopy(data, len, temp, decrypt.size, data.size - len)
        return temp
    }
}