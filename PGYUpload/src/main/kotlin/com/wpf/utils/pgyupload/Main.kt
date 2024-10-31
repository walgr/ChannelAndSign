package com.wpf.utils.pgyupload

import com.google.gson.Gson
import com.wpf.utils.pgyupload.pgy.PGYHtml
import com.wpf.utils.pgyupload.pgy.Upload
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入参数")
        return
    }
    var apiKey = ""
    var channel = ""
    var description = ""
    var buildType = "android"
    var apkPath = ""
    var uploadResultSavePath = ""
    var basePgyHtmlPath = ""
    var test = ""
    var host = ""
    var release = ""
    var delUploadApk = false
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请检查输入${arg.replace("-", "")}")
            return
        }
        if ("-apiKey" == arg) {
            apiKey = nextInput
        }
        if ("-buildType" == arg) {
            buildType = nextInput
        }
        if ("-apkPath" == arg) {
            apkPath = nextInput
        }
        if ("-uploadResultSavePath" == arg) {
            uploadResultSavePath = nextInput
        }
        if ("-basePgyHtmlPath" == arg) {
            basePgyHtmlPath = nextInput
        }
        if ("-test" == arg) {
            test = nextInput
        }
        if ("-host" == arg) {
            host = nextInput
        }
        if ("-release" == arg) {
            release = nextInput
        }
        if ("-channel" == arg) {
            channel = nextInput
        }
        if ("-description" == arg) {
            description = nextInput
        }
        if ("-delUploadApk" == arg) {
            delUploadApk = nextInput == "1"
        }
    }
    val apk = File(apkPath)
    if (apkPath.isEmpty() || !apk.exists() || apk.length() == 0L) {
        println("未发现上传的apk")
        exitProcess(-1)
    }
    println("开始上传蒲公英")
    Upload.uploadApk(apiKey, buildType, description, channel, apk) {
        if (it == true) {
            Upload.getUploadResult(apiKey, buildType, description, channel) { apkInfo ->
                if (apkInfo != null) {
                    println("获取发布状态完成，App蒲公英信息如下：")
                    val apkInfoJson = Gson().toJson(apkInfo)
                    val apkInfoStr = apkInfoJson
                        .replace("{", "")
                        .replace("}", "")
                        .replace("\"", "")
                        .replace(",", "\n")
                        .trim()
                    println(apkInfoStr)
                    PGYHtml.deal(apk.parent.replace("\\", "\\\\"), test, host, release, basePgyHtmlPath, apkInfo)
                    if (uploadResultSavePath.isNotEmpty()) {
                        val outFile = File(uploadResultSavePath)
                        if (!outFile.exists()) {
                            outFile.createNewFile()
                        }
                        outFile.writeText(apkInfoJson)
                    }
                    if (delUploadApk) {
                        apk.delete()
                    }
                    println("上传蒲公英成功")
                } else {
                    println("上传蒲公英失败")
                }
            }
        }
    }
}