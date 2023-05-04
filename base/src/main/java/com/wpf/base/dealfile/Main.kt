package com.wpf.base.dealfile

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请按照-baseChannel、-channelPath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword配置")
        return
    }
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.contains("-") && nextInput.contains("-")) {
            println("参数异常，请按照-baseChannel、-channelPath、-channelsPath、-zipalignPath、-signFile、-signPassword、-signAlias、-signAliasPassword配置")
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
    }
}