package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.checkWinPath
import com.wpf.utils.ex.createCheck
import com.wpf.utils.jiagu.utils.AES128Helper.encrypt
import com.wpf.utils.jiagu.utils.ApplicationHelper
import com.wpf.utils.jiagu.utils.DexHelper
import com.wpf.utils.jiagu.utils.EncryptUtils
import com.wpf.utils.tools.DXUtil
import com.wpf.utils.tools.SignHelper
import java.io.File
import java.util.zip.Deflater

object Jiagu {

    /**
     * 1. 处理原始dex随机位置数据修改 保存修改信息
     * 2. 修改dex后缀为jiagu
     * 3. 对保存的修改信息加密
     */
    fun deal(
        srcApkPath: String,
        privateKeyFilePath: String = "", publicKeyFilePath: String = "",
        signFilePath: String = "",
        signAlias: String = "",
        keyStorePassword: String = "",
        keyPassword: String = "",
        showLog: Boolean = false,
    ) {
        if (srcApkPath.isEmpty()) {
            println("该文件地址为空！")
            return
        }
        val srcApkFile = File(srcApkPath)
        if (!srcApkFile.exists() || !srcApkFile.canRead() || srcApkFile.extension != "apk") {
            println("该文件非apk或者不可读取：${srcApkPath}")
            return
        }
        if (showLog) {
            println("开始加固：${srcApkPath}")
        }
        val cachePathFile = File(srcApkFile.parent + File.separator + "cache").createCheck(false)
        val jiaguApkFile =
            File(srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension + "_jiagu." + srcApkFile.extension)
        srcApkFile.copyTo(jiaguApkFile, true)
        val jiaguApkZip = ZipArchive(jiaguApkFile.toPath())
        val srcDexList = DexHelper.getApkSrcDexList(jiaguApkZip)

        val jiaguReleaseAAR = ResourceManager.getResourceFile("jiagulibrary-release.aar")
        val jiaguReleaseAARPath = cachePathFile.path + File.separator + "jiaguReleaseAAR"
        FileUtil.unZipFiles(jiaguReleaseAAR, jiaguReleaseAARPath)
        jiaguReleaseAAR.delete()
        val jiaguReleaseJarPath = jiaguReleaseAARPath + File.separator + "classes.jar"
        val jiaguReleaseDexPath = jiaguReleaseAARPath + File.separator + "classes.dex"
        DXUtil.jar2Dex(jiaguReleaseJarPath.checkWinPath(), jiaguReleaseDexPath.checkWinPath())
        val jiaguReleaseDexFile = File(jiaguReleaseDexPath)
        if (!jiaguReleaseDexFile.exists() || jiaguReleaseDexFile.length() == 0L) {
            println("未找到壳Dex")
            File(jiaguReleaseAARPath).deleteRecursively()
            return
        }
        //添加.so
        val soFileList = mutableMapOf(
            "libjiagu_64.so" to File(jiaguReleaseAARPath + File.separator + "jni/arm64-v8a/libjiagu.so"),
        )
        if (jiaguApkZip.getInputStream("lib/armeabi-v7a") != null) {
            soFileList["libjiagu.so"] = File(jiaguReleaseAARPath + File.separator + "jni/armeabi-v7a/libjiagu.so")
        }
        if (jiaguApkZip.getInputStream("lib/x86") != null) {
            soFileList["libjiagu.so"] = File(jiaguReleaseAARPath + File.separator + "jni/x86/libjiagu.so")
        }
        if (jiaguApkZip.getInputStream("lib/x86_64") != null) {
            soFileList["libjiagu.so"] = File(jiaguReleaseAARPath + File.separator + "jni/x86_64/libjiagu.so")
        }
        soFileList.forEach {
            jiaguApkZip.add(
                BytesSource(
                    it.value.toPath(), "assets/" + it.key, Deflater.BEST_COMPRESSION
                )
            )
        }

        val keDexByteArray = jiaguReleaseDexFile.readBytes()
        File(jiaguReleaseAARPath).deleteRecursively()

        val srcApplicationName = ApplicationHelper.getName(srcApkFile) ?: ""
        if (srcApplicationName.isEmpty()) {
            println("获取原始ApplicationName失败")
            return
        }
        var tempDex = ByteArray(1 + srcApplicationName.length)
        tempDex[0] = srcApplicationName.length.toByte()
        System.arraycopy(srcApplicationName.toByteArray(), 0, tempDex, 1, srcApplicationName.length) // app的application名

        var encryptData: ByteArray? = null
        srcDexList.forEachIndexed { index, pair ->
            val dexByteArray = pair.second.readBytes()
            if (index == 0) {
                tempDex =
                    tempDex.copyOf(tempDex.size + 4 + dexByteArray.size) // 扩容 1字节application名长度 + app的application名 + 4字节源dex大小 + 源dex
                System.arraycopy(
                    EncryptUtils.intToByteArray(dexByteArray.size),
                    0,
                    tempDex,
                    tempDex.size - 4 - dexByteArray.size,
                    4
                ) // 4字节源dex大小
                System.arraycopy(dexByteArray, 0, tempDex, tempDex.size - dexByteArray.size, dexByteArray.size) // 源dex
                encryptData = encrypt(tempDex, 512)
            } else {
                encryptData = encryptData!!.copyOf(encryptData!!.size + 4 + dexByteArray.size) // 扩容
                System.arraycopy(
                    EncryptUtils.intToByteArray(dexByteArray.size),
                    0,
                    encryptData!!,
                    encryptData!!.size - 4 - dexByteArray.size,
                    4
                ) // 4字节源dex大小
                tempDex = EncryptUtils.encryptXor(dexByteArray)
                System.arraycopy(tempDex, 0, encryptData!!, encryptData!!.size - dexByteArray.size, dexByteArray.size)
            }
        }
        // 合并dex
        val mergeDex = DexHelper.mergeDex(keDexByteArray, encryptData!!)
        //添加壳Dex
        val mainDexName = "classes.dex"
        //删除原包内所有dex
        srcDexList.forEach {
            jiaguApkZip.delete(it.first)
        }
        jiaguApkZip.add(
            BytesSource(
                mergeDex, mainDexName, Deflater.BEST_COMPRESSION
            )
        )

        //修改manifest的Application为解密的
        val androidManifest = "AndroidManifest.xml"
        val fixManifestFile = ApplicationHelper.setNewName(
            srcApkFile.parent,
            jiaguApkZip.getInputStream(androidManifest),
            "com.wpf.util.jiagulibrary.StubApp"
        )
        jiaguApkZip.delete(androidManifest)
        jiaguApkZip.add(
            BytesSource(
                fixManifestFile.toPath(), androidManifest, Deflater.DEFAULT_COMPRESSION
            )
        )
        fixManifestFile.delete()
        jiaguApkZip.close()
        //签名
        if (signFilePath.isNotEmpty()) {
            if (showLog) {
                println("正在对加固包签名")
            }
            SignHelper.sign(
                signFilePath.checkWinPath(),
                signAlias,
                keyStorePassword,
                keyPassword,
                "",
                jiaguApkFile.path.checkWinPath(),
                true
            )
            if (showLog) {
                println("签名完成： ${jiaguApkFile.path.replace(".apk", "_sign.apk")}")
            }
        }
        cachePathFile.deleteRecursively()
        if (showLog) {
            println("加固完成：${srcApkPath}")
        }
    }
}