package com.wpf.base.dealfile

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请按照-baseChannel、-channelPath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword、-filePath、-dealSign配置")
        return
    }
    var filePath = ""
    var dealSign = true
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请按照-baseChannel、-channelPath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword、-filePath、-dealSign配置")
            return
        }
        if ("-baseChannel" == arg) {
            channelBaseInsertFilePath = nextInput
        }
        if ("-channelPath" == arg) {
            channelSavePath = nextInput
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
    }
    println("开启处理...")
    ChannelAndSign.scanFile(true, inputFilePath = filePath, dealSign = dealSign) {}
}