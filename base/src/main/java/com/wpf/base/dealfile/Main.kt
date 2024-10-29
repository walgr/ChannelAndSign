package com.wpf.base.dealfile

import com.wpf.utils.ResourceManager

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入")
        return
    }
    var serviceBaseUrl = "http://0.0.0.0:8080/"
    var filePath = ""
    var fileFilter = ""
    var cachePath = ""
    var dealSign = true
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请检查输入:$arg")
            return
        }
        if ("-serviceBaseUrl" == arg) {
            serviceBaseUrl = nextInput
        }
        if ("-baseChannel" == arg) {
            channelBaseInsertFilePath = nextInput
        }
        if ("-channelsPath" == arg) {
            channelsFilePath = nextInput
        }
        if ("-signFile" == arg) {
            signFile = nextInput
        }
        if ("-signPassword" == arg) {
            signPassword = nextInput
        }
        if ("-signAlias" == arg) {
            signAlias = nextInput
        }
        if ("-signAliasPassword" == arg) {
            signAliasPassword = nextInput
        }
        if ("-filePath" == arg) {
            filePath = nextInput
        }
        if ("-fileFilter" == arg) {
            fileFilter = nextInput
        }
        if ("-dealSign" == arg) {
            dealSign = "1" == nextInput
        }
        if ("-channelSavePath" == arg) {
            channelSavePath = nextInput
        }
        if ("-cachePath" == arg) {
            cachePath = nextInput
        }
        if ("-delApkAfterSign" == arg) {
            delApkAfterSign = "1" == nextInput
        }
    }
    println("开始处理打渠道包...")
    ResourceManager.serverBaseUrl = serviceBaseUrl
    ResourceManager.cachePath = cachePath
    if (cachePath.isNotEmpty()) {
        println("设置ResourceManager缓存目录:${cachePath}")
    }
    val startTime = System.currentTimeMillis()
    ChannelAndSign.scanFile(inputFilePath = filePath, fileFilter = fileFilter, dealSign = dealSign) {
        println("处理完毕... 用时：${System.currentTimeMillis() - startTime}毫秒")
    }
    ResourceManager.delTemp()
}