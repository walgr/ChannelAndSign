package com.wpf.utils.jiagu

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入")
        return
    }
    var srcApkPath = ""
    var privateKeyFilePath = ""
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
    }
    println("开始加固")
    val startTime = System.currentTimeMillis()
    Jiagu.deal(srcApkPath, privateKeyFilePath)
    println("加固结束，用时:${System.currentTimeMillis() - startTime}")
}