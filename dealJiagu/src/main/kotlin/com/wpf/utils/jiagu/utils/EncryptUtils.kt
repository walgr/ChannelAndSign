package com.wpf.utils.jiagu.utils

object EncryptUtils {
    /**
     * dex头(也就是前112位进行异或)
     */
    fun encryptXor(data: ByteArray, len: Int = 112): ByteArray {
        for (i in 0 until len) {
            data[i] = (data[i].toInt() xor 0x66).toByte()
        }

        return data
    }

    /**
     * int转byte[]
     */
    fun intToByteArray(number: Int): ByteArray {
        var numberTemp = number
        val b = ByteArray(4)
        for (i in 3 downTo 0) {
            b[i] = (numberTemp and 0xFF).toByte()
            numberTemp = numberTemp shr 8
        }
        return b
    }
}