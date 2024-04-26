package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.checkWinPath
import com.wpf.utils.ex.createCheck
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import com.wpf.utils.jiagu.utils.AES128Helper.encrypt
import com.wpf.utils.jiagu.utils.ApplicationHelper
import com.wpf.utils.jiagu.utils.DexHelper
import com.wpf.utils.jiagu.utils.EncryptUtils
import com.wpf.utils.tools.DXUtil
import com.wpf.utils.tools.LogStreamThread
import com.wpf.utils.tools.SignHelper
import java.io.File
import java.util.zip.Deflater

object Jiagu {

    fun deal(
        srcApkPath: String,
        secretKey: String, keyVi: String,
        androidSdkPath: String,
        jdkPath: String = "",
        signFilePath: String = "",
        signAlias: String = "",
        keyStorePassword: String = "",
        keyPassword: String = "",
        showLog: Boolean = false,
    ) {
        kotlin.runCatching {
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

            val jiaguLibraryZip = ResourceManager.getResourceFile("jiaguLibrary.zip")
            val projectRootPath = cachePathFile.path + File.separator + "jiaguLibrary" + File.separator
            FileUtil.unZipFiles(jiaguLibraryZip, projectRootPath)
            ResourceManager.delResourceByPath(jiaguLibraryZip.path)
            if (androidSdkPath.isNotEmpty()) {
                val localPropertiesFile = File(projectRootPath + File.separator + "local.properties")
                localPropertiesFile.writeText("sdk.dir=${androidSdkPath.checkWinPath()}")
            }
            val aesCFile = File(projectRootPath + "/jiagulibrary/src/main/cpp/utils/aes.c".replace("/", File.separator))
            var aesCFileStr = aesCFile.readText()
            aesCFileStr = aesCFileStr.replace(
                "const char\\* AES_KEYCODE = \".*\";".toRegex(),
                "const char* AES_KEYCODE = \"$secretKey\";"
            )
            aesCFileStr =
                aesCFileStr.replace("const char\\* AES_IV = \".*\";".toRegex(), "const char* AES_IV = \"$keyVi\";")
            aesCFile.writeText(aesCFileStr)
            if (showLog) {
                println("正在打壳AAR")
            }
            if (isLinuxRuntime || isMacRuntime) {
                Runtime.getRuntime().exec(arrayOf("chmod", "-R", "+x", "${projectRootPath}gradlew")).waitFor()
            }
            val cmd =
                mutableListOf(
                    "${projectRootPath}gradlew" + if (isWinRuntime) ".bat" else "",
                    ":jiagulibrary:assembleRelease"
                )
            if (jdkPath.isNotEmpty()) {
                cmd.addAll(arrayOf("-D", "org.gradle.java.home=${jdkPath}"))
            }
            val process = ProcessBuilder(cmd).directory(File(projectRootPath)).start()
            LogStreamThread(process.inputStream, false).start()
            LogStreamThread(process.errorStream).start()
            val result = process.waitFor()
            if (showLog) {
                println(if (result == 0) "AAR打包成功" else "AAR打包失败")
            }
            if (result != 0) return

            val jiaguReleaseAAR = File(
                projectRootPath + "jiagulibrary/build/outputs/aar/".replace("/", File.separator)
            ).listFiles()?.first { it.extension == "aar" }
            if (jiaguReleaseAAR == null) {
                if (showLog) {
                    println("打包aar失败")
                }
                File(projectRootPath).deleteRecursively()
                return
            }
            if (showLog) {
                println("正在aar转dex")
            }
            val jiaguReleaseAARPath = cachePathFile.path + File.separator + "jiaguReleaseAAR"
            FileUtil.unZipFiles(jiaguReleaseAAR, jiaguReleaseAARPath)
            jiaguReleaseAAR.delete()
            val jiaguReleaseJarPath = jiaguReleaseAARPath + File.separator + "classes.jar"
            val jiaguReleaseDexPath = jiaguReleaseAARPath + File.separator + "classes.dex"
            DXUtil.jar2Dex(jiaguReleaseJarPath.checkWinPath(), jiaguReleaseDexPath.checkWinPath())
            val jiaguReleaseDexFile = File(jiaguReleaseDexPath)
            if (!jiaguReleaseDexFile.exists() || jiaguReleaseDexFile.length() == 0L) {
                if (showLog) {
                    println("未找到壳Dex")
                }
                File(jiaguReleaseAARPath).deleteRecursively()
                return
            }
            //添加.so
            if (showLog) {
                println("添加so到apk中")
            }
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
            File(projectRootPath).deleteRecursively()
            val srcApplicationName = ApplicationHelper.getName(srcApkFile) ?: ""
            if (srcApplicationName.isEmpty()) {
                if (showLog) {
                    println("获取原始ApplicationName失败")
                }
                return
            }
            if (showLog) {
                println("获取原始ApplicationName：${srcApplicationName}")
            }

            if (showLog) {
                println("合并原始apk中dex并加密")
            }
            var tempDex = ByteArray(1 + srcApplicationName.length)
            tempDex[0] = srcApplicationName.length.toByte()
            System.arraycopy(
                srcApplicationName.toByteArray(),
                0,
                tempDex,
                1,
                srcApplicationName.length
            ) // app的application名

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
                    System.arraycopy(
                        dexByteArray,
                        0,
                        tempDex,
                        tempDex.size - dexByteArray.size,
                        dexByteArray.size
                    ) // 源dex
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
                    System.arraycopy(
                        tempDex,
                        0,
                        encryptData!!,
                        encryptData!!.size - dexByteArray.size,
                        dexByteArray.size
                    )
                }
            }
            // 合并dex
            if (showLog) {
                println("合并加密dex到壳dex")
            }
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

            val stubAppName = "com.wpf.util.jiagulibrary.StubApp"
            if (showLog) {
                println("修改AndroidManifest中Android:name=${stubAppName}")
            }
            //修改manifest的Application为解密的
            val androidManifest = "AndroidManifest.xml"
            val fixManifestFile = ApplicationHelper.setNewName(
                srcApkFile.parent,
                jiaguApkZip.getInputStream(androidManifest),
                stubAppName
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
        }.getOrElse {
            File(File(srcApkPath).parent + File.separator + "cache").deleteRecursively()
            throw it
        }
    }
}