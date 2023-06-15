package com.wpf.base.dealfile

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请按照-baseChannel、-channelSavePath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword、-filePath、-dealSign配置")
        return
    }
    var filePath = ""
    var dealSign = true
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请按照-baseChannel、-channelSavePath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword、-filePath、-dealSign配置")
            return
        }
        if ("-baseChannel" == arg) {
            channelBaseInsertFilePath = nextInput
        }
        if ("-channelsPath" == arg) {
            channelsFilePath = nextInput
        }
        if ("-zipalignPath" == arg) {
            zipalignFile = nextInput
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
        if ("-dealSign" == arg) {
            dealSign = "1" == nextInput
        }
        if ("-channelSavePath" == arg) {
            channelSavePath = nextInput
        }
        if ("-delApkAfterSign" == arg) {
            delApkAfterSign = "1" == nextInput
        }
    }
//    delApkAfterSign = true
    println("开始处理...")
    val startTime = System.currentTimeMillis()
    ChannelAndSign.scanFile(inputFilePath = filePath, dealSign = dealSign) {
        println("处理完毕...用时：${System.currentTimeMillis() - startTime}")
    }
}