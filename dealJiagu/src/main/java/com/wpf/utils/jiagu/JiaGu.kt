package com.wpf.utils.jiagu

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.checkWinPath
import com.wpf.utils.ex.createCheck
import com.wpf.utils.ex.md5
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import com.wpf.utils.jiagu.utils.AES128Helper.encrypt
import com.wpf.utils.jiagu.utils.ApplicationHelper
import com.wpf.utils.jiagu.utils.DexHelper
import com.wpf.utils.jiagu.utils.EncryptUtils
import com.wpf.utils.tools.LogStreamThread
import com.wpf.utils.tools.SignHelper
import net.lingala.zip4j.ZipFile
import java.io.File
import java.util.zip.Deflater

object JiaGu {

    fun deal(
        srcApkPath: String,
        secretKey: String, keyVi: String,
        androidSdkPath: String,
        jdkPath: String = "",
        signFilePath: String = "",
        signAlias: String = "",
        keyStorePassword: String = "",
        keyPassword: String = "",
        isAndroidX: Boolean = true,
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
            val packageName = ApplicationHelper.getPackageName(srcApkFile)
            val srcApplicationName = ApplicationHelper.getName(srcApkFile) ?: ""
            val jiaguLibraryZip = ResourceManager.getResourceFile("jiaguLibrary.zip", overwrite = true)
            val jiaGuHashKey = arrayOf(
                srcApplicationName,
                secretKey,
                keyVi,
                androidSdkPath,
                jdkPath,
                jiaguLibraryZip.md5(),
            ).joinToString()
            if (showLog) {
                println("加固HashKey:$jiaGuHashKey")
            }
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
                println("开始加固：${srcApkPath}")
            }
            val tmpPathFile = File(srcApkFile.parent + File.separator + "tmp").createCheck(false)
            val jiaguApkFile =
                File(srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension + "_jiagu." + srcApkFile.extension)
            srcApkFile.copyTo(jiaguApkFile, true)
            var jiaguApkZipArchive = ZipArchive(jiaguApkFile.toPath())
            val jiaguCachePath =
                ResourceManager.cachePath + File.separator + "cache/jiagu/".replace("/", File.separator)
            val jiaguCacheHashPath = jiaguCachePath + File.separator + jiaGuHashKey.hashCode()
            File(jiaguCacheHashPath).createCheck(false)
            val jiaguDexCacheHashFile = File(jiaguCacheHashPath + File.separator + "classes.dex")
            val jiaguSoV8CacheHashFile = File(jiaguCacheHashPath + File.separator + "libjiagu_v8a.so")
            val jiaguSoV7CacheHashFile = File(jiaguCacheHashPath + File.separator + "libjiagu_v7a.so")
            val jiaguSoCacheHashFile = File(jiaguCacheHashPath + File.separator + "libjiagu.so")
            val jiaguSoX86CacheHashFile = File(jiaguCacheHashPath + File.separator + "libjiagu_x86.so")
            val jiaguSoX8664CacheHashFile = File(jiaguCacheHashPath + File.separator + "libjiagu_x86_64.so")

            if (!(jiaguDexCacheHashFile.exists() && (jiaguSoV8CacheHashFile.exists()
                        || jiaguSoV7CacheHashFile.exists()
                        || jiaguSoCacheHashFile.exists()
                        || jiaguSoX86CacheHashFile.exists())
                        || jiaguSoX8664CacheHashFile.exists())) {
                if (showLog) {
                    println("加固壳不存在，正在处理生成加固壳")
                }

                val projectRootPath = tmpPathFile.path + File.separator + "jiaguLibrary" + File.separator
                File(projectRootPath).deleteRecursively()
                FileUtil.unZipFiles(jiaguLibraryZip, projectRootPath)
                ResourceManager.delResourceByPath(jiaguLibraryZip.path)

                if (isAndroidX) {
                    if (showLog) {
                        println("是AndroidX,删除Android的CoreComponentFactory")
                    }
                    File("$projectRootPath/app/src/main/java/android".replace("/", File.separator)).deleteRecursively()
                } else {
                    if (showLog) {
                        println("非AndroidX,删除AndroidX的CoreComponentFactory")
                    }
                    File("$projectRootPath/app/src/main/java/androidx".replace("/", File.separator)).deleteRecursively()
                    File("$projectRootPath/app/build.gradle.kts".replace("/", File.separator)).apply {
                        val srcText = readText()
//                        if (showLog) {
//                            println("原始AppBuild配置：\n${srcText}")
//                        }
                        val desText = srcText.replace(
                            "implementation(\"androidx.annotation:annotation:1.7.0\")",
                            "implementation(\"com.android.support:support-annotations:28.0.0\")"
                        )
                        if (showLog) {
                            println("修改后AppBuild配置：\n${desText}")
                        }
                        writeText(desText)
                    }
                }

                if (showLog) {
                    println("修改秘钥")
                }
                //设置加固加密秘钥
                val aesCFile =
                    File(projectRootPath + "/jiagulibrary/src/main/cpp/utils/aes.c".replace("/", File.separator))
                var aesCFileStr = aesCFile.readText()
                aesCFileStr = aesCFileStr.replace(
                    "const char\\* AES_KEYCODE = \".*\";".toRegex(),
                    "const char* AES_KEYCODE = \"$secretKey\";"
                )
                aesCFileStr =
                    aesCFileStr.replace("const char\\* AES_IV = \".*\";".toRegex(), "const char* AES_IV = \"$keyVi\";")
                aesCFile.writeText(aesCFileStr)
//                if (showLog) {
//                    println("修改后秘钥代码:${aesCFileStr}")
//                }

                if (showLog) {
                    println("正在打加固壳Apk")
                }
                if (androidSdkPath.isNotEmpty()) {
                    val localPropertiesFile = File(projectRootPath + File.separator + "local.properties")
                    localPropertiesFile.writeText("sdk.dir=${androidSdkPath.checkWinPath()}")
                }
                if (isLinuxRuntime || isMacRuntime) {
                    Runtime.getRuntime().exec(arrayOf("chmod", "-R", "+x", "${projectRootPath}gradlew")).waitFor()
                }
                val cmdApk =
                    mutableListOf(
                        "${projectRootPath}gradlew" + if (isWinRuntime) ".bat" else "",
                        ":app:assembleRelease"
                    )
                if (jdkPath.isNotEmpty()) {
                    cmdApk.addAll(arrayOf("-D", "org.gradle.java.home=${jdkPath}"))
                }
                val processApk = ProcessBuilder(cmdApk).directory(File(projectRootPath)).start()
                LogStreamThread(processApk.inputStream, showLog).start()
                LogStreamThread(processApk.errorStream, showLog).start()
                val resultApk = processApk.waitFor()
                println(if (resultApk == 0) "Apk打包成功" else "Apk打包失败")
                if (resultApk != 0) {
                    jiaguApkFile.deleteRecursively()
                    File(File(srcApkPath).parent + File.separator + "tmp").deleteRecursively()
                    return
                }

                val jiaguAppFile = File(
                    projectRootPath + "app/build/outputs/apk/release/".replace("/", File.separator)
                ).listFiles()?.first {
                    it.extension == "apk"
                } ?: return
                val jiaguAppZip = ZipArchive(jiaguAppFile.toPath())
                val jiaguReleaseDexFile = File(jiaguAppFile.parent + File.separator + "classes.dex")
                jiaguAppZip.getInputStream("classes.dex")?.let {
                    FileUtil.save2File(it, jiaguReleaseDexFile)
                    it.close()
                }
                if (!jiaguReleaseDexFile.exists() || jiaguReleaseDexFile.length() == 0L) {
                    if (showLog) {
                        println("打加固壳失败")
                    }
                    tmpPathFile.deleteRecursively()
                    return
                }

                jiaguAppZip.getInputStream("lib/arm64-v8a/libjiagu.so")?.let {
                    FileUtil.save2File(
                        it,
                        File(jiaguAppFile.parent + File.separator + "jni/arm64-v8a/libjiagu.so").createCheck(true)
                    )
                    it.close()
                }
                jiaguReleaseDexFile.copyTo(jiaguDexCacheHashFile, true)
                File(jiaguAppFile.parent + File.separator + "jni/arm64-v8a/libjiagu.so").apply {
                    if (exists()) {
                        this.copyTo(jiaguSoV8CacheHashFile, true)
                    }
                }
                File(jiaguAppFile.parent + File.separator + "jni/armeabi-v7a/libjiagu.so").apply {
                    if (exists()) {
                        this.copyTo(jiaguSoV7CacheHashFile, true)
                    }
                }
                File(jiaguAppFile.parent + File.separator + "jni/armeabi/libjiagu.so").apply {
                    if (exists()) {
                        this.copyTo(jiaguSoCacheHashFile, true)
                    }
                }
                File(jiaguAppFile.parent + File.separator + "jni/x86/libjiagu.so").apply {
                    if (exists()) {
                        this.copyTo(jiaguSoX86CacheHashFile, true)
                    }
                }
                File(jiaguAppFile.parent + File.separator + "jni/x86_64/libjiagu.so").apply {
                    if (exists()) {
                        this.copyTo(jiaguSoX8664CacheHashFile, true)
                    }
                }
                File(projectRootPath).deleteRecursively()
            } else {
                if (showLog) {
                    println("发现加固壳缓存直接处理")
                }
            }
            //添加.so
            if (showLog) {
                println("添加so到apk中")
            }
            val soFileList = mutableMapOf(
                "libjiagu_v8a.so" to jiaguSoV8CacheHashFile,
            )
            if (jiaguApkZipArchive.getInputStream("lib/armeabi-v7a") != null) {
                soFileList["libjiagu_v7a.so"] = jiaguSoV7CacheHashFile
            }
            if (jiaguApkZipArchive.getInputStream("lib/armeabi") != null) {
                soFileList["libjiagu.so"] = jiaguSoCacheHashFile
            }
            if (jiaguApkZipArchive.getInputStream("lib/x86") != null) {
                soFileList["libjiagu_x86.so"] = jiaguSoX86CacheHashFile
            }
            if (jiaguApkZipArchive.getInputStream("lib/x86_64") != null) {
                soFileList["libjiagu_x86_64.so"] = jiaguSoX8664CacheHashFile
            }
            soFileList.forEach {
                jiaguApkZipArchive.add(
                    BytesSource(
                        it.value.toPath(), "assets/" + it.key, Deflater.BEST_COMPRESSION
                    )
                )
            }

            val keDexByteArray = jiaguDexCacheHashFile.readBytes()
            if (showLog) {
                println("合并原始apk中dex并加密")
            }
            //1字节application名长度 + app的application名
            var tempDex = ByteArray(1 + srcApplicationName.length)
            tempDex[0] = srcApplicationName.length.toByte()
            if (showLog) {
                println("写入原始apk中ApplicationName:${srcApplicationName}, 长度:${srcApplicationName.length}")
            }
            if (srcApplicationName.isNotEmpty()) {
                System.arraycopy(
                    srcApplicationName.toByteArray(),
                    0,
                    tempDex,
                    1,
                    srcApplicationName.length
                ) // app的application名
            }

            var encryptData: ByteArray? = null
            val srcDexList = DexHelper.getApkSrcDexList(jiaguApkZipArchive)
            srcDexList.forEachIndexed { index, pair ->
                val dexByteArray = pair.second.readBytes()
                val oldDataArray = DexHelper.dealAllFunctionInNopInDex(
                    dexByteArray,
                    whiteList = arrayOf(
                        (packageName ?: srcApplicationName.substring(
                            0,
                            srcApplicationName.lastIndexOf(".")
                        )).replace(".", "/")
                    ),
                    blackList = arrayOf("databinding")
                )
                if (index == 0) {
                    // 扩容 4字节源dex大小 + 源dex + 4字节源dex抽取代码大小 + 源dex抽取代码
                    tempDex = tempDex.copyOf(tempDex.size + 4 + dexByteArray.size + 4 + oldDataArray.size)
                    System.arraycopy(
                        EncryptUtils.intToByteArray(dexByteArray.size),
                        0,
                        tempDex,
                        tempDex.size - dexByteArray.size - 4 - oldDataArray.size - 4,
                        4
                    ) // 4字节源dex大小
                    System.arraycopy(
                        dexByteArray,
                        0,
                        tempDex,
                        tempDex.size - dexByteArray.size - oldDataArray.size - 4,
                        dexByteArray.size
                    ) // 源dex
                    System.arraycopy(
                        EncryptUtils.intToByteArray(oldDataArray.size),
                        0,
                        tempDex,
                        tempDex.size - oldDataArray.size - 4,
                        4
                    ) // 4字节源dex抽取代码大小
                    System.arraycopy(
                        oldDataArray,
                        0,
                        tempDex,
                        tempDex.size - oldDataArray.size,
                        oldDataArray.size
                    ) // 源dex抽取代码
                    encryptData = encrypt(tempDex, 512)
                } else {
                    // 扩容 4字节源dex大小 + 源dex + 4字节源dex抽取代码大小 + 源dex抽取代码
                    encryptData =
                        encryptData!!.copyOf(encryptData!!.size + 4 + dexByteArray.size + 4 + oldDataArray.size)
                    System.arraycopy(
                        EncryptUtils.intToByteArray(dexByteArray.size),
                        0,
                        encryptData!!,
                        encryptData!!.size - dexByteArray.size - 4 - oldDataArray.size - 4,
                        4
                    ) // 4字节源dex大小
                    tempDex = EncryptUtils.encryptXor(dexByteArray)
                    System.arraycopy(
                        tempDex,
                        0,
                        encryptData!!,
                        encryptData!!.size - dexByteArray.size - oldDataArray.size - 4,
                        dexByteArray.size
                    ) // 源dex
                    System.arraycopy(
                        EncryptUtils.intToByteArray(oldDataArray.size),
                        0,
                        encryptData!!,
                        encryptData!!.size - oldDataArray.size - 4,
                        4
                    ) // 4字节源dex抽取代码大小
                    System.arraycopy(
                        oldDataArray,
                        0,
                        encryptData!!,
                        encryptData!!.size - oldDataArray.size,
                        oldDataArray.size
                    ) // 源dex抽取代码
                }
            }
            // 合并dex
            if (showLog) {
                println("合并加密dex(${encryptData!!.size})到壳dex(${keDexByteArray.size})")
            }

            val mergeDex = DexHelper.mergeDex(keDexByteArray, encryptData!!)

            //添加壳Dex
            val mainDexName = "classes.dex"
            if (showLog) {
                println("合并后${mainDexName}:${mergeDex.size}")
            }
            //删除原包内所有dex
            val srcDexNameList = srcDexList.map {
                it.first
            }
            srcDexList.forEach {
                it.second.close()
            }
            jiaguApkZipArchive.close()
            var jiaguApkZip = ZipFile(jiaguApkFile)
            jiaguApkZip.removeFiles(srcDexNameList)
            jiaguApkZip.close()
            jiaguApkZipArchive = ZipArchive(jiaguApkFile.toPath())
            jiaguApkZipArchive.add(
                BytesSource(
                    mergeDex, mainDexName, Deflater.BEST_COMPRESSION
                )
            )

            val stubAppName = "com.wpf.util.jiagulibrary.StubApp"
            if (showLog) {
                println("修改AndroidManifest为壳Application:${stubAppName}")
            }
            //修改manifest的Application为解密的
            val androidManifest = "AndroidManifest.xml"
            val fixManifestFile = ApplicationHelper.setNewName(
                srcApkFile.parent,
                jiaguApkZipArchive.getInputStream(androidManifest),
                stubAppName
            )
            jiaguApkZipArchive.close()
            jiaguApkZip = ZipFile(jiaguApkFile)
            jiaguApkZip.removeFile(androidManifest)
            jiaguApkZip.close()
            jiaguApkZipArchive = ZipArchive(jiaguApkFile.toPath())
            jiaguApkZipArchive.add(
                BytesSource(
                    fixManifestFile.toPath(), androidManifest, Deflater.BEST_COMPRESSION
                )
            )
            fixManifestFile.delete()
            jiaguApkZipArchive.close()
            if (showLog) {
                println("加固完成：${srcApkPath},加固包：${jiaguApkFile.path}")
            }
            //签名
            if (signFilePath.isNotEmpty()) {
                if (showLog) {
                    println("正在对加固包签名")
                }
                val signOutFile = SignHelper.sign(
                    signFilePath.checkWinPath(),
                    signAlias,
                    keyStorePassword,
                    keyPassword,
                    outApkPath = "",
                    inputApkPath = jiaguApkFile.path.checkWinPath(),
                    false
                )
                if (signOutFile.isNotEmpty()) {
                    if (showLog) {
                        println("签名完成：$signOutFile")
                    }
                } else {
                    if (showLog) {
                        println("签名失败")
                    }
                }
            }
            tmpPathFile.deleteRecursively()
        }.onFailure {
            File(File(srcApkPath).parent + File.separator + "tmp").deleteRecursively()
            if (showLog) {
                println(it.message)
                it.printStackTrace()
            }
            throw it
        }
    }
}