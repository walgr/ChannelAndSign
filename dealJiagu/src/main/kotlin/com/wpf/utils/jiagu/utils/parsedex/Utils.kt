package com.wpf.utils.jiagu.utils.parsedex

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.Adler32

object Utils {
    /**
     * 大端
     */
    fun byte2int(bytes: ByteArray): Int {
        val targets: Int =
            ((bytes[3].toInt() and 0xFF) shl 24) or ((bytes[2].toInt() and 0xFF) shl 16) or ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[0].toInt() and 0xFF)
        return targets
    }

    fun int2Byte(integer: Int): ByteArray {
        val byteNum = (40 - Integer.numberOfLeadingZeros(if (integer < 0) integer.inv() else integer)) / 8
        val byteArray = ByteArray(4)

        for (n in 0 until byteNum) byteArray[3 - n] = (integer ushr (n * 8)).toByte()

        return (byteArray)
    }

    fun short2Byte(number: Short): ByteArray {
        var temp = number.toInt()
        val b = ByteArray(2)
        for (i in b.indices) {
            b[i] = (temp and 0xff.toByte().toInt()).toByte() //将最低位保存在最低位
            temp = temp shr 8 // 向右移8位
        }
        return b
    }

    fun byte2Short(byteArray: ByteArray): Short {
        val s0 = (byteArray[0].toInt() and 0xff).toShort()
        var s1 = (byteArray[1].toInt() and 0xff).toShort()
        s1 = (s1.toInt() shl 8).toShort()
        return (s0.toInt() or s1.toInt()).toShort()
    }

    fun bytesToHexString(src: ByteArray?): String? {
        //byte[] src = reverseBytes(src1);
        val stringBuilder = StringBuilder("")
        if (src == null || src.isEmpty()) {
            return null
        }
        for (i in src.indices) {
            val v = src[i].toInt() and 0xFF
            val hv = Integer.toHexString(v)
            if (hv.length < 2) {
                stringBuilder.append(0)
            }
            stringBuilder.append("$hv ")
        }
        return stringBuilder.toString()
    }

    fun getChars(bytes: ByteArray): CharArray {
        val cs = Charset.forName("UTF-8")
        val bb = ByteBuffer.allocate(bytes.size)
        bb.put(bytes)
        bb.flip()
        val cb = cs.decode(bb)
        return cb.array()
    }

    fun copyByte(src: ByteArray?, start: Int, len: Int): ByteArray? {
        if (src == null) {
            return null
        }
        if (start > src.size) {
            return null
        }
        if ((start + len) > src.size) {
            return null
        }
        if (start < 0) {
            return null
        }
        if (len <= 0) {
            return null
        }
        val resultByte = ByteArray(len)
        for (i in 0 until len) {
            resultByte[i] = src[i + start]
        }
        return resultByte
    }

    fun reverseBytes(bytess: ByteArray): ByteArray {
        val bytes = ByteArray(bytess.size)
        for (i in bytess.indices) {
            bytes[i] = bytess[i]
        }
        if ((bytes.size % 2) != 0) {
            return bytes
        }
        var i = 0
        val len = bytes.size
        while (i < (len / 2)) {
            val tmp = bytes[i]
            bytes[i] = bytes[len - i - 1]
            bytes[len - i - 1] = tmp
            i += 1
        }
        return bytes
    }

    fun filterStringNull(str: String): String {
        val strByte = str.toByteArray()
        val newByte = ArrayList<Byte>()
        for (i in strByte.indices) {
            if (strByte[i].toInt() != 0) {
                newByte.add(strByte[i])
            }
        }
        val newByteAry = ByteArray(newByte.size)
        for (i in newByteAry.indices) {
            newByteAry[i] = newByte[i]
        }
        return String(newByteAry)
    }

    fun getStringFromByteAry(srcByte: ByteArray?, start: Int): String {
        if (srcByte == null) {
            return ""
        }
        if (start < 0) {
            return ""
        }
        if (start >= srcByte.size) {
            return ""
        }
        var `val` = srcByte[start]
        var i = 1
        val byteList = ArrayList<Byte>()
        while (`val`.toInt() != 0) {
            byteList.add(srcByte[start + i])
            `val` = srcByte[start + i]
            i += 1
        }
        val valAry = ByteArray(byteList.size)
        for (j in byteList.indices) {
            valAry[j] = byteList[j]
        }
        try {
            return String(valAry, charset("UTF-8"))
        } catch (e: Exception) {
            println("encode error:$e")
            return ""
        }
    }

    /**
     * 读取C语言中的uleb类型
     * 目的是解决整型数值浪费问题
     * 长度不固定，在1~5个字节中浮动
     * @param srcByte
     * @param offset
     * @return
     */
    fun readUnsignedLeb128(srcByte: ByteArray?, offset: Int): ByteArray {
        var offsetTemp = offset
        val byteAryList: MutableList<Byte> = ArrayList()
        var bytes = copyByte(srcByte, offsetTemp, 1)!![0]
        var highBit = (bytes.toInt() and 0x80).toByte()
        byteAryList.add(bytes)
        offsetTemp += 1
        while (highBit.toInt() != 0) {
            bytes = copyByte(srcByte, offsetTemp, 1)!![0]
            highBit = (bytes.toInt() and 0x80).toByte()
            offsetTemp += 1
            byteAryList.add(bytes)
        }
        val byteAry = ByteArray(byteAryList.size)
        for (j in byteAryList.indices) {
            byteAry[j] = byteAryList[j]
        }
        return byteAry
    }

    /**
     * 生成leb128数据
     */
    fun encodeULeb128(value: Int): ByteArray {
        var valueTemp = value
        val out = ByteArrayOutputStream()
        var remaining: Int = valueTemp shr 7
        while (remaining != 0) {
            out.write(((valueTemp and 0x7f) or 0x80))
            valueTemp = remaining
            remaining = remaining shr 7
        }
        out.write((valueTemp and 0x7f))
        return out.toByteArray()
    }

    /**
     * 解码leb128数据
     * 每个字节去除最高位，然后进行拼接，重新构造一个int类型数值，从低位开始
     * @param byteAry
     * @return
     */
    fun decodeULeb128(byteAry: ByteArray): Int {
        var result = 0
        var cur: Int
        var count = 0
        do {
            cur = byteAry[count].toInt() and 0xff
            result = result or ((cur and 0x7f) shl (count * 7))
            count++
        } while (count < byteAry.size)
        return result
    }

    //用来覆盖字节数组
    fun replaceBytes(
        sourceByte: ByteArray,
        replaceByte: ByteArray,
        offset: Int
    ): ByteArray {
        val oldData = ByteArray(replaceByte.size)
        for (i in replaceByte.indices) {
            oldData[i] = sourceByte[offset + i]
            sourceByte[offset + i] = replaceByte[i]
        }
        return oldData
    }

    /**
     * 修改dex头 sha1值
     *
     * @param dexBytes
     * @throws NoSuchAlgorithmException
     */
    @Throws(NoSuchAlgorithmException::class)
    fun updateSHA1Header(dexBytes: ByteArray) {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(dexBytes, 32, dexBytes.size - 32) // 从32到结束计算sha-1
        val newdt = md.digest()
        System.arraycopy(newdt, 0, dexBytes, 12, 20) // 修改sha-1值（12-31）
    }

    /**
     * 修改dex头 file_size值
     *
     * @param dexBytes
     */
    fun updateFileSizeHeader(dexBytes: ByteArray) {
        // 新文件长度
        val newfs = intToByte(dexBytes.size)

        // 高位低位交换
        for (i in 0..1) {
            val tmp = newfs[i]
            newfs[i] = newfs[newfs.size - 1 - i]
            newfs[newfs.size - 1 - i] = tmp
        }
        System.arraycopy(newfs, 0, dexBytes, 32, 4) // 修改（32-35）
    }

    /**
     * 修改dex头，CheckSum 校验码
     *
     * @param dexBytes
     */
    fun updateCheckSumHeader(dexBytes: ByteArray) {
        val adler = Adler32()
        adler.update(dexBytes, 12, dexBytes.size - 12) // 从12到文件末尾计算校验码
        val value = adler.value
        val va = value.toInt()
        val newcs = intToByte(va)

        for (i in 0..1) {
            val tmp = newcs[i]
            newcs[i] = newcs[newcs.size - 1 - i]
            newcs[newcs.size - 1 - i] = tmp
        }

        System.arraycopy(newcs, 0, dexBytes, 8, 4) // 效验码赋值（8-11）
    }

    /**
     * int 转byte[]
     *
     * @param number
     * @return
     */
    fun intToByte(number: Int): ByteArray {
        var number = number
        val b = ByteArray(4)
        for (i in 3 downTo 0) {
            b[i] = (number % 256).toByte()
            number = number shr 8
        }
        return b
    }
}

fun main() {

}
