package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.google.gson.Gson
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.createCheck
import com.wpf.utils.tools.ManifestEditorUtil
import net.dongliu.apk.parser.ApkParsers
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.Deflater
import kotlin.math.min
import kotlin.random.Random

class ApkConfig(
    private val srcApplicationName: String,
    val dexInfoList: MutableList<DexInfo> = mutableListOf()
) {
    class DexInfo(
        private val dexName: String,
        private val dexMd5: String,
        val dealList: MutableList<DealInfo> = mutableListOf(),
    ) {
        class DealInfo(
            private val stepStartPos: Long,
            private val stepEndPos: Long,
            private val dealStartPos: Long,
            private val dealLength: Int,
            private val srcBytes: ByteArray,
        )
    }
}


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
        val srcDexList = jiaguApk.listEntries().filter {
            it.endsWith("dex")
        }
        val srcDexInputStreamMap = srcDexList.map {
            it to jiaguApk.getInputStream(it).buffered()
        }
        val srcDexMd5Map: Map<String, String> = mapOf(*srcDexList.map {
            it to DigestUtils.md5Hex(jiaguApk.getInputStream(it))
        }.toTypedArray())
        val srcManifestFileStr = ApkParsers.getManifestXml(srcApkFile)
        val applicationStr = "(?<=<application)(.*?)(?=>)".toRegex().find(srcManifestFileStr)!!.value
        val apkConfig = ApkConfig("(?<=android:name=\")(.*?)(?=\")".toRegex().find(applicationStr)?.value ?: "")
        val jiaguConfigFile = File(srcApkFile.parent + File.separator + "jiagu.config").createCheck(true)
        val configModelList = mutableListOf<ApkConfig.DexInfo>()
        val step = 1024 * 1024
        val jiaguFragmentLength = 100
        srcDexInputStreamMap.forEach {
            val dexName = it.first
            val inputStream = it.second
            println("处理${dexName}")
            val jiaguDexFile =
                File(cachePathFile.path + File.separator + dexName.replace(".dex", ".wpfjiagu")).createCheck(true)
            jiaguDexFile.writeText("")
            val jiaguDexOutputStream = RandomAccessFile(jiaguDexFile, "rw")
            val configModel = ApkConfig.DexInfo(dexName, srcDexMd5Map[dexName]!!)
            var startPos = 0L
            val allBytes = inputStream.readBytes()
            val allCount = allBytes.size
            do {
                println("正在处理${startPos}-${startPos + step}")
                val randomPos = Random.nextInt(min(step - jiaguFragmentLength, allCount - startPos.toInt()))
                println("获取随机位置${startPos + randomPos}")
                val readBytesForFragment =
                    allBytes.sliceArray(startPos.toInt() + randomPos until startPos.toInt() + randomPos + jiaguFragmentLength)
                println("获取${readBytesForFragment.size}长度的数据保存到配置文件中")
                repeat(jiaguFragmentLength) { pos ->
                    allBytes[startPos.toInt() + randomPos + pos] = 0
                }
                configModel.dealList.add(
                    ApkConfig.DexInfo.DealInfo(
                        startPos,
                        startPos + step,
                        startPos + randomPos,
                        jiaguFragmentLength,
                        readBytesForFragment
                    )
                )
                startPos += step
            } while (startPos < allCount)
            jiaguDexOutputStream.write(allBytes)
            configModelList.add(configModel)
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
        apkConfig.dexInfoList.addAll(configModelList)
        jiaguConfigFile.writeText(Gson().toJson(apkConfig))
        jiaguApk.add(
            BytesSource(
                jiaguConfigFile.toPath(), "assets/" + jiaguConfigFile.name, Deflater.DEFAULT_COMPRESSION
            )
        )
        //添加解密的主dex
        javaClass.getResourceAsStream("/classes.dex")?.let {
            jiaguApk.add(
                BytesSource(
                    it, "classes.dex", Deflater.DEFAULT_COMPRESSION
                )
            )
            it.close()
        }
        //修改manifest的Application为解密的
        val androidManifest = "AndroidManifest.xml"
        val srcManifestFile = File(srcApkFile.parent + File.separator + "AndroidManifest_src.xml").createCheck(true)
        val androidManifestIS = jiaguApk.getInputStream(androidManifest)
        FileUtil.save2File(androidManifestIS, srcManifestFile)
        androidManifestIS.close()
        val fixManifestFile = File(srcApkFile.parent + File.separator + androidManifest).createCheck(true)
        ManifestEditorUtil.doCommand(
            mutableListOf(
                srcManifestFile.path,
                "-f",
                "-o",
                fixManifestFile.path,
                "-an",
                "com.wpf.util.jiadulibrary.StubApplication"
            )
        )
        jiaguApk.delete(androidManifest)
        jiaguApk.add(
            BytesSource(
                fixManifestFile.toPath(), androidManifest, Deflater.DEFAULT_COMPRESSION
            )
        )
        srcManifestFile.delete()
        fixManifestFile.delete()

        jiaguConfigFile.delete()
        jiaguApk.close()
        cachePathFile.deleteRecursively()
    }
}