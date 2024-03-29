package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.utils.ex.createCheck
import java.io.File
import java.util.zip.Deflater
import kotlin.math.min
import kotlin.random.Random

object Jiagu {

    /**
     * 1. 处理原始dex随机位置数据修改 保存修改信息
     * 2. 修改dex后缀为jiagu
     * 3. 对保存的修改信息加密
     */
    fun deal(srcApkPath: String, privateKeyFilePath: String = "") {
        if (srcApkPath.isEmpty()) {
            throw IllegalArgumentException("该文件地址为空！")
        }
        val srcApkFile = File(srcApkPath)
        if (!srcApkFile.exists() || !srcApkFile.canRead() || srcApkFile.extension != "apk") {
            throw IllegalArgumentException("该文件非apk或者不可读取：${srcApkPath}")
        }
        val cachePathFile = File(srcApkFile.parent + File.separator + "cache").createCheck(false)
        val jiaguApkFile =
            File(srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension + "_jiagu." + srcApkFile.extension)
        srcApkFile.copyTo(jiaguApkFile, true)
        val jiaguApk = ZipArchive(jiaguApkFile.toPath())
        val srcDexInputStreamMap = jiaguApk.listEntries().filter {
            it.endsWith("dex")
        }.map {
            it to jiaguApk.getInputStream(it).buffered()
        }
        val jiaguConfigFile = File(srcApkFile.parent + File.separator + "jiagu.config").createCheck(true)
        val configStrB = StringBuilder()
        val step = 1024 * 1024
        val jiaguFragmentLength = 100
        val split = ";"
        srcDexInputStreamMap.forEach {
            val dexName = it.first
            val inputStream = it.second
            println("处理${dexName}")
            val jiaguDexFile =
                File(cachePathFile.path + File.separator + dexName.replace(".dex", ".wpfjiagu")).createCheck(true)
            val jiaguDexOutputStream = jiaguDexFile.outputStream()
            configStrB.append(dexName + split)
            var startPos = 0
            var stop = false
            var readCount = 0
            var writeCount = 0L
            do {
                println("正在处理${startPos}-${startPos + step}")
                var randomPos = Random.nextInt(step - jiaguFragmentLength)
                println("获取随机位置${randomPos}")
                var readBytesForJiagu = ByteArray(randomPos)
                var readSize = inputStream.read(readBytesForJiagu)
                if (readSize > 0) {
                    readCount += readSize
                } else {
                    stop = true
                    println("已经处理完当前文件了")
                    continue
                }
                println("从位置:${startPos}读取长度：${randomPos}的数据，实际读取了${readSize}的数据")
                val readBytesForFragment: ByteArray
                var remainSize: Int
                if (readSize < randomPos) {
                    println("未读取到随机位置，说明剩余数据长度小于随机位置长度，下面再次进行随机")
                    randomPos = Random.nextInt(readSize - jiaguFragmentLength)
                    println("获取新随机位置${randomPos}")
                    readBytesForFragment = readBytesForJiagu.copyOfRange(randomPos, randomPos + jiaguFragmentLength)
                    readBytesForJiagu = readBytesForJiagu.copyOfRange(0, randomPos)
                    remainSize = readSize - randomPos - jiaguFragmentLength
                    println("从读取的数据中：[${startPos}-${startPos + readSize}]裁剪新的数据：[${startPos}-${startPos + randomPos}]，剩余长度:${remainSize}")
                    jiaguDexOutputStream.write(readBytesForJiagu)
                    writeCount += readBytesForJiagu.size
                    println("写入新裁剪的数据长度：${readBytesForJiagu.size}到jiagu文件中")
                    jiaguDexOutputStream.write(ByteArray(readBytesForFragment.size))
                    writeCount += readBytesForFragment.size
                    println("写入空数据长度：${readBytesForFragment.size}到jiagu文件中")
                    stop = true
                } else {
                    jiaguDexOutputStream.write(readBytesForJiagu)
                    writeCount += readBytesForJiagu.size
                    println("写入读取的数据到jiagu文件中")
                    readBytesForFragment = ByteArray(jiaguFragmentLength)
                    readSize = inputStream.read(readBytesForFragment)
                    if (readSize > 0) {
                        readCount += readSize
                    }
                    println("读取长度：${readSize}的数据到片段中")
                    jiaguDexOutputStream.write(ByteArray(jiaguFragmentLength))
                    writeCount += jiaguFragmentLength
                    println("写入空数据长度：${jiaguFragmentLength}到jiagu文件中")
                    remainSize = step - randomPos - readSize
                    if (readSize < 0) {
                        stop = true
                    }
                }
                configStrB.append(
                    "[${startPos}-${startPos + step}-${startPos + randomPos.toLong()}-${
                        min(
                            jiaguFragmentLength, readSize
                        )
                    }]" + split
                )
                configStrB.append(readBytesForFragment.joinToString())
                println("写入片段到日志中，长度：${readBytesForFragment.size}")
                if (remainSize > 0) {
                    println("写入剩下的数据长度：${remainSize}到jiagu文件")
                    readBytesForJiagu = ByteArray(remainSize)
                    readSize = inputStream.read(readBytesForJiagu)
                    if (readSize > 0) {
                        readCount += readSize
                    }
                    jiaguDexOutputStream.write(readBytesForJiagu)
                    writeCount += readBytesForJiagu.size
                }
                startPos += step
            } while (!stop)
            println("总共读取了${readCount}")
            println("总共写入了${writeCount}")
            configStrB.append("\n")
            inputStream.close()
            jiaguDexOutputStream.close()
            jiaguApk.delete(dexName)
            jiaguApk.add(
                BytesSource(
                    jiaguDexFile.toPath(), "assets/" + jiaguDexFile.name, Deflater.DEFAULT_COMPRESSION
                )
            )
            jiaguDexFile.delete()
        }
        jiaguConfigFile.writeText(configStrB.toString())
        jiaguApk.add(
            BytesSource(
                jiaguConfigFile.toPath(), "assets/" + jiaguConfigFile.name, Deflater.DEFAULT_COMPRESSION
            )
        )
        jiaguConfigFile.delete()
        jiaguApk.close()
        cachePathFile.deleteRecursively()
    }
}