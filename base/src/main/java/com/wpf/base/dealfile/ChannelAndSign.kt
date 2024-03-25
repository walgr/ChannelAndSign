package com.wpf.base.dealfile

import com.android.zipflinger.BytesSource
import com.android.zipflinger.ZipArchive
import com.wpf.base.dealfile.util.ThreadPoolHelper
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.createCheck
import com.wpf.utils.ex.subString
import com.wpf.utils.tools.AXMLEditor2Util
import com.wpf.utils.tools.SignHelper
import net.dongliu.apk.parser.ApkParsers
import java.io.File
import java.util.concurrent.Callable
import java.util.zip.Deflater
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess

object ChannelAndSign {
    private val defaultChannelName: String by lazy {
        if (channelBaseInsertFilePath.isEmpty()) return@lazy ""
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(channelBaseInsertFilePath)
            .getElementsByTagName("meta-data").item(0).attributes.item(1).nodeValue
    }

    //原始文件路径
    private var inputFilePath: String = ""
    private val noSignApkList = mutableListOf<String>()

    fun scanFile(
        inputFilePath: String,
        fileFilter: String = "",
        dealSign: Boolean = true,
        exitProcess: Boolean = true,
        finish: (() -> Unit)
    ) {
        if (inputFilePath.isEmpty()) {
            println("输入的文件路径不正确")
            finish.invoke()
            return
        }
        this.inputFilePath = inputFilePath
        try {
            dealScanFile(inputFilePath, fileFilter, dealSign) {
                finish.invoke()
                if (exitProcess) {
                    exitProcess(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("运行错误:${e.message}")
            finish.invoke()
            if (exitProcess) {
                exitProcess(-6457)
            }
        }
    }

    private fun dealChannel() = !(channelsFilePath.isEmpty() || channelBaseInsertFilePath.isEmpty())

    private fun dealScanFile(
        inputFilePath: String, fileFilter: String = "", dealSign: Boolean = true, finish: (() -> Unit)
    ) {
        val dealFile = File(inputFilePath)
        if (dealFile.exists() && dealFile.isFile && "apk" == dealFile.extension) {
            println("处理文件:${dealFile.name}")
            noSignApkList.add(dealFile.path)
            val curPath = dealFile.parent + File.separator
            dealChannel(dealFile) {
                val channelPath = getChannelPath(curPath).ifEmpty { curPath }
                signPath(dealSign, channelPath, !dealChannel(), finish)
            }
        } else if (dealFile.isDirectory) {
            val apkFileList = dealFile.listFiles()?.filter {
                it.isFile && "apk" == it.extension && (fileFilter.isEmpty() || it.name.contains(fileFilter))
            }
            apkFileList?.forEach {
                noSignApkList.add(it.path)
            }
            ThreadPoolHelper.run(runnable = apkFileList?.map {
                println("处理文件:${it.name}")
                Callable {
                    dealChannel(it)
                }
            }) { results ->
                results?.forEach {
                    it?.forEach { log ->
                        println(log)
                    }
                }
                if (apkFileList?.isNotEmpty() == true) {
                    val channelPath = getChannelPath(inputFilePath).ifEmpty { inputFilePath }
                    signPath(dealSign, channelPath, !dealChannel(), finish)
                } else {
                    println("目录下未找到apk")
                    finish.invoke()
                }
            }
        } else {
            println("未找到要处理的文件")
            finish.invoke()
        }
    }

    private fun getChannelPath(curPath: String): String {
        var channelPath = channelSavePath
        if (channelPath.isNotEmpty()) {
            if (!channelPath.endsWith(File.separator)) channelPath += File.separator
            if (!(channelPath.startsWith(File.separator) || channelPath.contains(":"))) channelPath =
                curPath + channelPath
        }
        return channelPath
    }

    /**
     * 处理加固包打渠道包
     */
    private fun dealChannel(inputApk: File, finish: (() -> Unit)? = null): List<String> {
        //如果是渠道包
        if (inputApk.nameWithoutExtension.contains("_") && !inputApk.nameWithoutExtension.contains("_jiagu")) return arrayListOf(
            "已是渠道文件，不需处理"
        )
        if (!dealChannel()) return arrayListOf("没有渠道文件，不处理：${inputApk.path}")
        val logList = arrayListOf<String>()
        val curPath = inputApk.parent
        val inputZipFile = ZipFile(inputApk)
        //创建渠道包存储文件夹
        val channelPath = getChannelPath(curPath)
        val channelPathFile = File(channelPath)
        channelPathFile.mkdirs()

        //解压得到AndroidManifest.xml
        val baseManifestFile =
            File(curPath + File.separator + inputApk.nameWithoutExtension + File.separator + "AndroidManifest.xml").createCheck(
                true
            )
        FileUtil.save2File(inputZipFile.getInputStream(inputZipFile.getEntry("AndroidManifest.xml")), baseManifestFile)
        logList.add("解压 ${inputApk.name} 得到AndroidManifest.xml")

        //先去除旧的渠道数据
        val outNoChannelFile =
            File(curPath + File.separator + inputApk.nameWithoutExtension + File.separator + "AndroidManifestNoChannel.xml").createCheck(
                true
            )
        AXMLEditor2Util.doCommandTagDel(
            "meta-data", "UMENG_CHANNEL", baseManifestFile.path, outNoChannelFile.path
        )
        logList.add("去除原渠道并重命名为AndroidManifestNoChannel.xml")

        val channelsFile = File(channelsFilePath)
        ThreadPoolHelper.run(runnable = channelsFile.readLines().map {
            Callable {
                dealChannelApk(it, curPath, outNoChannelFile, channelPath, inputApk)
            }
        }) { results ->
            results?.forEach {
                it?.forEach { log ->
                    println(log)
                }
            }
            File(curPath + File.separator + inputApk.nameWithoutExtension + File.separator + "cache").delete()
            inputZipFile.close()
            baseManifestFile.delete()
            outNoChannelFile.parentFile.delete()
            outNoChannelFile.delete()
            outNoChannelFile.parentFile.delete()
            finish?.invoke()
        }
        return logList
    }

    private fun dealChannelApk(
        it: String, curPath: String, outNoChannelFile: File, channelPath: String, inputApkPath: File
    ): List<String> {
        val logList = arrayListOf<String>()
        val fields = it.split(" ")
        val channelName: String = fields[2].trim().replace("\n", "")
        val channelApkFileName: String = fields[1].trim().replace("\n", "")
        logList.add("当前处理渠道：${channelName}")
        val outNoChannelFileNew =
            File(curPath + File.separator + inputApkPath.nameWithoutExtension + File.separator + "cache" + File.separator + outNoChannelFile.nameWithoutExtension + "_" + channelName + ".xml")
        outNoChannelFile.copyTo(outNoChannelFileNew, true)
        val baseManifestChannelFilePath =
            curPath + File.separator + inputApkPath.nameWithoutExtension + File.separator + "cache" + File.separator + channelName
        val baseManifestFileNew = File(baseManifestChannelFilePath + File.separator + "AndroidManifest.xml")
        outNoChannelFileNew.copyTo(baseManifestFileNew, true)
        //修改渠道数据
        val baseInsertFile = File(channelBaseInsertFilePath)
        val newChannelInsertFile =
            File(curPath + File.separator + inputApkPath.nameWithoutExtension + File.separator + "insert_${channelName}.xml")
        baseInsertFile.copyTo(newChannelInsertFile, true)
        //更新新渠道文件内渠道
        newChannelInsertFile.writeText(
            newChannelInsertFile.readText().replace(defaultChannelName, channelName)
        )
        //插入渠道信息
        AXMLEditor2Util.doCommandTagInsert(
            newChannelInsertFile.path, outNoChannelFileNew.path, baseManifestFileNew.path
        )
        //用完删除新渠道文件
        newChannelInsertFile.delete()
        outNoChannelFileNew.delete()

        val newChannelApkFile =
            File(channelPath.ifEmpty { curPath } + File.separator + "${inputApkPath.nameWithoutExtension}_${channelApkFileName}" + ".apk")
        inputApkPath.copyTo(newChannelApkFile, true)

        //更新新渠道AndroidManifest.xml到渠道apk中
        val newChannelApkZipFile = ZipArchive(newChannelApkFile.toPath())
        newChannelApkZipFile.delete("AndroidManifest.xml")
        newChannelApkZipFile.add(
            BytesSource(
                baseManifestFileNew.toPath(), "AndroidManifest.xml", Deflater.DEFAULT_COMPRESSION
            )
        )
        newChannelApkZipFile.close()
        baseManifestFileNew.delete()
        File(baseManifestChannelFilePath).delete()
        logList.add("apk已更新渠道信息，并保存到${newChannelApkZipFile.path.toAbsolutePath()}")
        val newChannelApkXmlStr = ApkParsers.getManifestXml(newChannelApkFile)
        if (newChannelApkXmlStr.isNotEmpty()) {
            val apkChannelName =
                Regex("<meta-data android:name=\"UMENG_CHANNEL\" android:value=\".*\" />").find(newChannelApkXmlStr)?.value?.subString(
                    "android:value=\"",
                    "\""
                )
            logList.add("获取渠道apk内渠道信息:${apkChannelName}")
            if (channelName != apkChannelName) {
                logList.add("获取渠道apk内渠道信息和渠道不一致，请排查问题!!!")
                throw RuntimeException("获取渠道apk内渠道信息(${apkChannelName})和渠道${channelName}不一致，请排查问题!!!")
            }
        } else {
            logList.add("获取渠道apk内渠道信息:失败，请排查问题!!!")
        }
        logList.add("")
        return logList
    }

    private fun signPath(
        dealSign: Boolean = true,
        inputApkPath: String,
        outputNew: Boolean = false,
        finish: (() -> Unit)? = null
    ) {
        if (!dealSign) {
            finish?.invoke()
            return
        }
        val dealFile = File(inputApkPath)
        if (dealFile.isDirectory) {
            val dealFiles = dealFile.listFiles()?.filter {
                it.isFile && "apk" == it.extension
            }
            ThreadPoolHelper.run(runnable = dealFiles?.map {
                Callable {
                    signApk(it, outputNew)
                }
            }) { results ->
                results?.forEach {
                    it?.forEach { log ->
                        println(log)
                    }
                }
                finish?.invoke()
            }
        } else {
            signApk(dealFile, outputNew).forEach {
                println(it)
            }
            finish?.invoke()
        }
    }

    private fun signApk(inputFile: File, outputNew: Boolean = false): List<String> {
        if (noSignApkList.contains(inputFile.path) && !outputNew) return arrayListOf("原始Apk文件:${inputFile.path}，不处理")
        if (!inputFile.isFile || "apk" != inputFile.extension) return arrayListOf("非Apk文件，未处理")
        val logList = arrayListOf<String>()
        if (SignHelper.sign(
                signFile = signFile,
                signAlias = signAlias,
                keyStorePassword = signPassword,
                keyPassword = signAliasPassword,
                outApkPath = if (outputNew) inputFile.path.replace(
                    inputFile.nameWithoutExtension,
                    inputFile.nameWithoutExtension + "_sign"
                ) else "",
                inputApkPath = inputFile.path,
                reserveInput = noSignApkList.contains(inputFile.path)
            )
        ) {
            logList.add("签名成功：$inputFile")
        } else {
            logList.add("签名失败：$inputFile")
        }
        return logList
    }
}