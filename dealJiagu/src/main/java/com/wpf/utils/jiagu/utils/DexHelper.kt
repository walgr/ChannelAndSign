package com.wpf.utils.jiagu.utils

import com.android.zipflinger.ZipArchive
import com.wpf.utils.jiagu.utils.parsedex.ParseDexUtils
import com.wpf.utils.jiagu.utils.parsedex.Utils
import com.wpf.utils.jiagu.utils.parsedex.struct.CodeItem
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.Adler32

object DexHelper {

    fun getApkSrcDexList(apk: ZipArchive): List<Pair<String, InputStream>> {
        val srcDexList = apk.listEntries().filter {
            it.endsWith("dex")
        }.sortedBy {
            (it.replace("classes", "").replace(".dex", "").ifEmpty { "1" }).toInt()
        }
        return srcDexList.map {
            it to apk.getInputStream(it)
        }
    }

    fun dealAllFunctionInNopInDex(
        dexBytes: ByteArray,
        whiteList: Array<String>? = null,
        blackList: Array<String>? = null,
        maxDealSize: Int = Integer.MAX_VALUE,
    ): ByteArray {
        val parseDexHelper = ParseDexUtils()
        parseDexHelper.parseAll(dexBytes)
        val allFunctionMap = mutableMapOf<String, CodeItem>().apply {
            putAll(parseDexHelper.directMethodCodeItemMap)
            putAll(parseDexHelper.virtualMethodCodeItemMap)
        }
        val byteBuffer = ByteArrayOutputStream()
        var count = 0
        allFunctionMap.forEach { (funName, codeItem) ->
            if (count++ < maxDealSize) {
                if (!whiteList.isNullOrEmpty() && whiteList.find { funName.contains(it, false) } == null) {
                    return@forEach
                }
                if (!blackList.isNullOrEmpty() && blackList.find { funName.contains(it, false) } != null) {
                    return@forEach
                }
                val nopBytes = ByteArray(codeItem.insns_size * 2)
                repeat(codeItem.insns_size * 2) {
                    nopBytes[it] = 0
                }
                val oldData = Utils.replaceBytes(dexBytes, nopBytes, codeItem.insnsOffset)
                byteBuffer.write(EncryptUtils.intToByteArray(codeItem.insnsOffset))
                byteBuffer.write(EncryptUtils.intToByteArray(nopBytes.size))
                byteBuffer.write(oldData)
            }

        }
//        fixDex(dexBytes)
        return byteBuffer.toByteArray()
    }

    fun mergeDex(mainDex: ByteArray, mergeDex: ByteArray): ByteArray {
        val temp = ByteArray(mainDex.size + mergeDex.size + 4)
        System.arraycopy(mainDex, 0, temp, 0, mainDex.size) // 壳dex
        System.arraycopy(mergeDex, 0, temp, mainDex.size, mergeDex.size) // 加密数据
        System.arraycopy(
            EncryptUtils.intToByteArray(mainDex.size), 0, temp,
            mainDex.size + mergeDex.size, 4
        ) // 4字节壳dex大小
        fixDex(temp)
        return temp
    }

    fun mergeDex(mainDex: ByteArray, mergeDexList: List<ByteArray>): ByteArray {
        var mergeDexAllSize = 0
        mergeDexList.forEach {
            mergeDexAllSize += it.size
        }
        val temp = ByteArray(mainDex.size + mergeDexAllSize + 4)
        System.arraycopy(mainDex, 0, temp, 0, mainDex.size) // 壳dex
        var lastMergeDexSize = 0
        mergeDexList.forEach { mergeDex ->
            System.arraycopy(mergeDex, lastMergeDexSize, temp, mainDex.size, mergeDex.size) // 加密数据
            lastMergeDexSize = mergeDex.size
        }

        System.arraycopy(
            mainDex.size.toByte(), 0, temp,
            mainDex.size + mergeDexAllSize, 4
        ) // 4字节壳dex大小
        fixDex(temp)
        return temp
    }

    /**
     * 处理dex
     *
     * @param newDex
     *
     * @throws NoSuchAlgorithmException
     */
    @Throws(NoSuchAlgorithmException::class)
    private fun fixDex(newDex: ByteArray) {
        //修改DEX file size文件头
        fixFileSizeHeader(newDex)
        //修改DEX SHA1 文件头
        fixSHA1Header(newDex)
        //修改DEX CheckSum文件头
        fixCheckSumHeader(newDex)
    }

    /**
     * 修改dex头，CheckSum 校验码
     *
     * @param dexBytes
     */
    private fun fixCheckSumHeader(dexBytes: ByteArray) {
        val adler = Adler32()
        adler.update(dexBytes, 12, dexBytes.size - 12) //从12到文件末尾计算校验码
        val value = adler.value
        val va = value.toInt()
        val newcs: ByteArray = EncryptUtils.intToByteArray(va)
        val recs = ByteArray(4)
        for (i in 0..3) {
            recs[i] = newcs[newcs.size - 1 - i]
        }
        System.arraycopy(recs, 0, dexBytes, 8, 4) //效验码赋值（8-11）
    }

    /**
     * 修改dex头 sha1值
     *
     * @param dexBytes
     *
     * @throws NoSuchAlgorithmException
     */
    @Throws(NoSuchAlgorithmException::class)
    private fun fixSHA1Header(dexBytes: ByteArray) {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(dexBytes, 32, dexBytes.size - 32) //从32位到结束计算sha--1
        val newdt = md.digest()
        System.arraycopy(newdt, 0, dexBytes, 12, 20) //修改sha-1值（12-31）
    }

    /**
     * 修改dex头 file_size值
     *
     * @param dexBytes
     */
    private fun fixFileSizeHeader(dexBytes: ByteArray) {
        //新文件长度
        val newfs: ByteArray = EncryptUtils.intToByteArray(dexBytes.size)
        val refs = ByteArray(4)
        //高位在前，低位在前掉个个
        for (i in 0..3) {
            refs[i] = newfs[newfs.size - 1 - i]
        }
        System.arraycopy(refs, 0, dexBytes, 32, 4) //修改（32-35）
    }

}
