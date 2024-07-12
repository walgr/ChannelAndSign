package com.wpf.utils.jiagu.utils

import com.wpf.utils.jiagu.utils.AES128Helper.getRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor

object AES128Helper {
    var DEFAULT_SECRET_KEY = "183ehypp8zaokmvf"
    var KEY_VI = "7dtu4vzwdwosceej"

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

    fun getRandom(length: Int): String {
        val arr = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
        )
        var result = arr[floor(Math.random() * 36).toInt()].toString()
        for (i in 1 until length) {
            result += arr[floor(Math.random() * 36).toInt()]
        }
        return result
    }
}

fun main() {
    println(getRandom(16))
    println(getRandom(16))
}