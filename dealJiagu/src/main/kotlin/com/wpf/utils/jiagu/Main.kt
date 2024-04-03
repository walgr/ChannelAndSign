package com.wpf.utils.jiagu

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入")
        return
    }
    var srcApkPath = ""
    var privateKeyFilePath = ""
    var signFilePath = ""
    var signAlias = ""
    var keyStorePassword = ""
    var keyPassword = ""
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请检查输入:$arg")
            return
        }
        if ("-srcApk" == arg) {
            srcApkPath = nextInput
        }
        if ("-privateKeyFilePath" == arg) {
            privateKeyFilePath = nextInput
        }
        if ("-signFilePath" == arg) {
            signFilePath = nextInput
        }
        if ("-signAlias" == arg) {
            signAlias = nextInput
        }
        if ("-keyStorePassword" == arg) {
            keyStorePassword = nextInput
        }
        if ("-keyPassword" == arg) {
            keyPassword = nextInput
        }
    }
    println("开始加固")
    val startTime = System.currentTimeMillis()
    Jiagu.deal(srcApkPath, privateKeyFilePath, signFilePath, signAlias, keyStorePassword, keyPassword)
    println("加固结束，用时:${System.currentTimeMillis() - startTime}")
}