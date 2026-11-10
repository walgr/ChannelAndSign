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
import com.wpf.utils.jiagu.d2c.D2cCompiler
import com.wpf.utils.jiagu.d2c.D2cRules
import com.wpf.utils.tools.LogStreamThread
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
        d2cConfigPath: String = "",
    ) {
        if (File(srcApkPath).extension.equals("xapk", true)) {
            val input = File(srcApkPath).absoluteFile
            val output = File(input.parentFile, input.nameWithoutExtension +
                if (signFilePath.isBlank()) "_jiagu.xapk" else "_jiagu_sign.xapk")
            val sidecar = File(input.parentFile, input.nameWithoutExtension + ".d2c.txt")
            val rules = d2cConfigPath.ifBlank { sidecar.takeIf { it.isFile }?.path ?: "" }
            XapkArchive.transform(input, output) { members, base ->
                println("XAPK：加固主 APK ${base.path}，保留 ${members.size - 1} 个拆分 APK 和附带资源")
                val baseWork = java.nio.file.Files.createTempDirectory(base.file.parentFile.toPath(), ".base-jiagu-").toFile()
                try {
                    val source = base.file.copyTo(File(baseWork, "base.apk"))
                    deal(source.path, secretKey, keyVi, androidSdkPath, jdkPath,
                        isAndroidX = isAndroidX, showLog = showLog, d2cConfigPath = rules)
                    val hardened = File(baseWork, "base_jiagu.apk")
                    check(hardened.isFile) { "XAPK 主 APK 加固未产生输出" }
                    XapkArchive.publish(hardened, base.file)
                } finally { baseWork.deleteRecursively() }
                if (signFilePath.isNotBlank()) members.forEach { member ->
                    val target = java.nio.file.Files.createTempFile(member.file.parentFile.toPath(), ".signed-", ".apk").toFile()
                    val signed = PackageSigner.sign(signFilePath, signAlias, keyStorePassword, keyPassword,
                        outApkPath = target.path, inputApkPath = member.file.path, androidSdkPath = androidSdkPath)
                    XapkArchive.publish(File(signed), member.file)
                }
                if (signFilePath.isNotBlank()) check(members.map { PackageSigner.certificate(it.file) }.distinct().size == 1) {
                    "XAPK 内 APK 签名证书不一致"
                }
            }
            println("XAPK 加固完成：$output" + if (signFilePath.isBlank()) "；安装前请对整个 XAPK 统一签名" else "")
            return
        }
        kotlin.runCatching {
            require(secretKey.toByteArray(Charsets.UTF_8).size == 16 &&
                    keyVi.toByteArray(Charsets.UTF_8).size == 16) {
                "AES-128 密钥和 IV 必须各为 16 个 UTF-8 字节"
            }
            if (srcApkPath.isEmpty()) {
                println("该文件地址为空！")
                return
            }
            val srcApkFile = File(srcApkPath)
            if (!srcApkFile.exists() || !srcApkFile.canRead() || !srcApkFile.extension.equals("apk", true)) {
                println("该文件非apk或者不可读取：${srcApkPath}")
                return
            }
            val packageName = ApplicationHelper.getPackageName(srcApkFile)
            val srcApplicationName = ApplicationHelper.getName(srcApkFile) ?: ""
            require(srcApplicationName != "com.wpf.util.jiagulibrary.StubApp") { "该 APK 已加固，请使用原始 APK，避免重复包壳" }
            require(srcApplicationName.toByteArray(Charsets.UTF_8).size in 1..255) { "Application 类名的 UTF-8 长度必须为 1..255 字节" }
            // Explicit exact-method rules override automatic selection under the manifest package.
            val rulesFile = if (d2cConfigPath.isNotBlank()) File(d2cConfigPath)
                else File(srcApkFile.parentFile, srcApkFile.nameWithoutExtension + ".d2c.txt")
            require(d2cConfigPath.isBlank() || rulesFile.isFile) { "D2C config not found: $rulesFile" }
            val d2c = run {
                val explicitRules = if (rulesFile.isFile) D2cRules.read(rulesFile) else null
                val original = ZipArchive(srcApkFile.toPath())
                val inputs = try {
                    DexHelper.getApkSrcDexList(original).associate { (name, stream) -> name to stream.use { it.readBytes() } }
                } finally { original.close() }
                require("classes.dex" in inputs) { "主 APK 缺少根目录 classes.dex，不能加固" }
                val rules = explicitRules ?: D2cCompiler.selectDefaults(inputs,
                    requireNotNull(packageName?.takeIf { it.isNotBlank() }) { "无法获取 APK 主包名，不能生成默认 D2C 白名单" }
                ).also { selection ->
                    println("D2C 默认白名单：$packageName 及其子包；可转换 ${selection.methods.size} 个方法，跳过 ${selection.skipped.size} 个不支持的方法")
                    if (showLog) selection.skipped.values.forEach { println("D2C 跳过：$it") }
                }.methods
                D2cCompiler.compileHybrid(inputs, rules).also {
                    println("混合保护：D2C ${it.d2cMethods.size} 个方法，VMP ${it.vmpMethods.size} 个方法；其余方法继续使用第三代方法抽离")
                }.takeIf { it.methods.isNotEmpty() }
            }
            val d2cHash = d2c?.let {
                java.security.MessageDigest.getInstance("SHA-256").digest(it.source.toByteArray(Charsets.UTF_8))
                    .joinToString("") { value -> "%02x".format(value.toInt() and 255) }
            } ?: ""
            val jiaguLibraryZip = ResourceManager.getResourceFile("jiaguLibrary.zip", overwrite = true)
            val integrityKey = IntegritySeal.key(File(ResourceManager.cachePath, "cache/jiagu-integrity"))
            val integrityPublicKey = IntegritySeal.publicHex(integrityKey)
            val jiaGuHashKey = arrayOf(
                srcApplicationName,
                secretKey,
                keyVi,
                androidSdkPath,
                jdkPath,
                jiaguLibraryZip.md5(),
                d2cHash,
                integrityPublicKey,
            ).joinToString()
            if (showLog) {
                println("加固缓存ID:${jiaGuHashKey.hashCode()}")
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
            val jiaguApkFile =
                File(srcApkFile.parent + File.separator + srcApkFile.nameWithoutExtension + "_jiagu." + srcApkFile.extension)
            srcApkFile.copyTo(jiaguApkFile, true)
            var jiaguApkZipArchive = ZipArchive(jiaguApkFile.toPath())
            val jiaguCachePath =
                ResourceManager.cachePath + File.separator + "cache/jiagu/".replace("/", File.separator)
            val jiaguCacheHashPath = jiaguCachePath + File.separator + jiaGuHashKey.hashCode() +
                if (d2cHash.isEmpty()) "" else "_d2c_$d2cHash"
            File(jiaguCacheHashPath).createCheck(false)
            val jiaguDexCacheHashFile = File(jiaguCacheHashPath + File.separator + "classes.dex")
            val shellSoFiles = mapOf(
                "arm64-v8a" to File(jiaguCacheHashPath, "libjiagu_v8a.so"),
                "armeabi-v7a" to File(jiaguCacheHashPath, "libjiagu_v7a.so"),
                "x86" to File(jiaguCacheHashPath, "libjiagu_x86.so"),
                "x86_64" to File(jiaguCacheHashPath, "libjiagu_x86_64.so"),
            )

            if (!jiaguDexCacheHashFile.isFile || jiaguDexCacheHashFile.length() == 0L ||
                shellSoFiles.values.any { !it.isFile || it.length() == 0L }) {
                if (showLog) {
                    println("加固壳不存在，正在处理生成加固壳")
                }

                // Keep CMake/Ninja paths short even when the APK is nested in an extracted XAPK.
                val shellWork = java.nio.file.Files.createTempDirectory("jg-").toFile()
                try {
                    val projectRootPath = shellWork.path + File.separator
                    FileUtil.unZipFiles(jiaguLibraryZip, projectRootPath)
                    File(projectRootPath, "jiagulibrary/src/main/java/com/wpf/util/jiagulibrary/utils/IntegrityVerifier.java").apply {
                        val source = readText(Charsets.UTF_8)
                        check(source.contains("PUBLIC_KEY_HEX = \"\"")) { "加固壳缺少完整性公钥注入点" }
                        writeText(source.replace("PUBLIC_KEY_HEX = \"\"", "PUBLIC_KEY_HEX = \"$integrityPublicKey\""), Charsets.UTF_8)
                    }
                    if (d2c != null) {
                        File(projectRootPath, "jiagulibrary/src/main/cpp/utils/d2c_generated.cpp")
                            .writeText(d2c.source, Charsets.UTF_8)
                    }
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
    //                        if (showLog) {
    //                            println("修改后AppBuild配置：\n${desText}")
    //                        }
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
                    fun cBytes(value: String) = value.toByteArray(Charsets.UTF_8)
                        .joinToString("") { "\\x%02x".format(it.toInt() and 0xff) }
                    aesCFileStr = "const char\\* AES_KEYCODE = \".*\";".toRegex().replace(aesCFileStr) {
                        "const char* AES_KEYCODE = \"${cBytes(secretKey)}\";"
                    }
                    aesCFileStr = "const char\\* AES_IV = \".*\";".toRegex().replace(aesCFileStr) {
                        "const char* AES_IV = \"${cBytes(keyVi)}\";"
                    }
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
                        error("加固壳编译失败：退出码 $resultApk")
                    }

                    val jiaguAppFile = File(
                        projectRootPath + "app/build/outputs/apk/release/".replace("/", File.separator)
                    ).listFiles()?.first {
                        it.extension == "apk"
                    } ?: error("加固壳编译未产生 APK")
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
                        error("加固壳编译未产生有效 DEX")
                    }

                    try {
                        // Extract every ABI from the newly built shell APK, including Intel.
                        shellSoFiles.forEach { (abi, cachedSo) ->
                            val soStream = requireNotNull(jiaguAppZip.getInputStream("lib/$abi/libjiagu.so")) {
                                "加固壳缺少 $abi/libjiagu.so"
                            }
                            soStream.use { FileUtil.save2File(it, cachedSo) }
                            check(cachedSo.length() > 0L) { "加固壳 $abi/libjiagu.so 为空" }
                        }
                        jiaguReleaseDexFile.copyTo(jiaguDexCacheHashFile, true)
                    } finally {
                        jiaguAppZip.close()
                    }
                } finally { shellWork.deleteRecursively() }
            } else {
                if (showLog) {
                    println("发现加固壳缓存直接处理")
                }
            }
            //添加.so
            if (showLog) {
                println("添加so到apk中")
            }
            // APKs often omit ZIP directory entries. Detect actual native-library files.
            val apkEntries = jiaguApkZipArchive.listEntries()
            val nativeAbis = apkEntries.filter { it.startsWith("lib/") && it.endsWith(".so") }
                .map { it.removePrefix("lib/").substringBefore('/') }.toSet()
            // Native-bridge emulators can run ARM APKs inside an Intel ART process.
            val targetAbis = setOf("arm64-v8a", "x86", "x86_64") + nativeAbis
            val soFileList = shellSoFiles.filterKeys { it in targetAbis }
            check(soFileList.isNotEmpty()) { "原 APK 没有可用的加固壳 ABI: $targetAbis" }
            soFileList.values.forEach { soFile ->
                jiaguApkZipArchive.add(
                    BytesSource(soFile.toPath(), "assets/" + soFile.name, Deflater.BEST_COMPRESSION)
                )
            }
            val keDexByteArray = jiaguDexCacheHashFile.readBytes()
            if (showLog) {
                println("合并原始apk中dex并加密")
            }
            //1字节application名长度 + app的application名
            val applicationBytes = srcApplicationName.toByteArray(Charsets.UTF_8)
            var tempDex = ByteArray(1 + applicationBytes.size)
            tempDex[0] = applicationBytes.size.toByte()
            if (showLog) {
                println("写入原始apk中ApplicationName:${srcApplicationName}, 长度:${srcApplicationName.length}")
            }
            if (srcApplicationName.isNotEmpty()) {
                System.arraycopy(
                    applicationBytes,
                    0,
                    tempDex,
                    1,
                    applicationBytes.size
                ) // app的application名
            }

            var encryptData: ByteArray? = null
            val srcDexList = DexHelper.getApkSrcDexList(jiaguApkZipArchive)
            srcDexList.forEachIndexed { index, pair ->
                val dexByteArray = d2c?.dexFiles?.get(pair.first) ?: pair.second.readBytes()
                val oldDataArray = DexHelper.dealAllFunctionInNopInDex(
                    dexByteArray,
                    whiteList = arrayOf(
                        (packageName ?: srcApplicationName.take(srcApplicationName.lastIndexOf("."))).replace(".", "/")
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
                    encryptData = encrypt(tempDex, 512, secretKey = secretKey, keyIv = keyVi)
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
            IntegritySeal.seal(jiaguApkFile, integrityKey)
            if (showLog) {
                println("加固完成：${srcApkPath},加固包：${jiaguApkFile.path}")
            }
            //签名
            if (signFilePath.isNotEmpty()) {
                if (showLog) {
                    println("正在对加固包签名")
                }
                val signOutFile = PackageSigner.sign(
                    signFilePath.checkWinPath(),
                    signAlias,
                    keyStorePassword,
                    keyPassword,
                    outApkPath = "",
                    inputApkPath = jiaguApkFile.path.checkWinPath(),
                    reserveInput = false,
                    androidSdkPath = androidSdkPath
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
        }.onFailure {
            if (showLog) {
                println(it.message)
                it.printStackTrace()
            }
            throw it
        }
    }
}
