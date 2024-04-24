package com.wpf.utils.jiagu.utils

import com.android.zipflinger.ZipArchive
import java.io.InputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.zip.Adler32

object DexHelper {
    fun getApkSrcDexList(jiaguApk: ZipArchive): List<Pair<String, InputStream>> {
        val srcDexList = jiaguApk.listEntries().filter {
            it.endsWith("dex")
        }.sortedBy {
            (it.replace("classes", "").replace(".dex", "").ifEmpty { "1" }).toInt()
        }
        return srcDexList.map {
            it to jiaguApk.getInputStream(it)
        }
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
     * @param newdex
     *
     * @throws NoSuchAlgorithmException
     */
    @Throws(NoSuchAlgorithmException::class)
    private fun fixDex(newdex: ByteArray) {
        //修改DEX file size文件头
        fixFileSizeHeader(newdex)
        //修改DEX SHA1 文件头
        fixSHA1Header(newdex)
        //修改DEX CheckSum文件头
        fixCheckSumHeader(newdex)
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
