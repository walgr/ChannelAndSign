package com.wpf.utils.jiagu

import com.wpf.utils.ResourceManager

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入")
        return
    }
    var serviceBaseUrl = "http://0.0.0.0:8080/"
    var srcApkPath = ""
    var secretKey = ""
    var secretKeyVi = ""
    var sdkPath = ""
    var jdkPath = ""
    var signFilePath = ""
    var signAlias = ""
    var keyStorePassword = ""
    var keyPassword = ""
    var cachePath = ""
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请检查输入:$arg")
            return
        }
        if ("-serviceBaseUrl" == arg) {
            serviceBaseUrl = nextInput
        }
        if ("-srcApk" == arg) {
            srcApkPath = nextInput
        }
        if ("-secretKey" == arg) {
            secretKey = nextInput
        }
        if ("-secretKeyVi" == arg) {
            secretKeyVi = nextInput
        }
        if ("-sdkPath" == arg) {
            sdkPath = nextInput
        }
        if ("-jdkPath" == arg) {
            jdkPath = nextInput
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
        if ("-cachePath" == arg) {
            cachePath = nextInput
        }
    }
    ResourceManager.serverBaseUrl = serviceBaseUrl
    ResourceManager.cachePath = cachePath
    println("开始加固")
    val startTime = System.currentTimeMillis()
    Jiagu.deal(
        srcApkPath = srcApkPath,
        secretKey = secretKey,
        keyVi = secretKeyVi,
        androidSdkPath = sdkPath,
        jdkPath = jdkPath,
        signFilePath = signFilePath,
        signAlias = signAlias,
        keyStorePassword = keyStorePassword,
        keyPassword = keyPassword
    )
    println("加固结束，用时:${System.currentTimeMillis() - startTime}")
}