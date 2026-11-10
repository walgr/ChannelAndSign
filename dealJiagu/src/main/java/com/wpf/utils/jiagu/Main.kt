package com.wpf.utils.jiagu

import com.wpf.utils.ResourceManager
import com.wpf.utils.jiagu.utils.formatTime

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
    var isAndroidX = true
    var isDebug = false
    var d2cConfigPath = ""
    var obfuscateResourcePaths = false
    var encryptResourceContents = false
    var encryptNativeLibraries = false
    var nativeExclusions = emptySet<String>()
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
        if ("-isAndroidX" == arg) {
            isAndroidX = nextInput == "1"
        }
        if ("-isDebug" == arg) {
            isDebug = nextInput == "1"
        }
        if ("-d2cConfig" == arg) {
            d2cConfigPath = nextInput
        }
        if ("-obfuscateResourcePaths" == arg) {
            require(nextInput == "0" || nextInput == "1") { "-obfuscateResourcePaths 需要 0 或 1" }
            obfuscateResourcePaths = nextInput == "1"
        }
        if ("-encryptResourceContents" == arg) {
            require(nextInput == "0" || nextInput == "1") { "-encryptResourceContents 需要 0 或 1" }
            encryptResourceContents = nextInput == "1"
        }
        if ("-encryptNativeLibraries" == arg) {
            require(nextInput == "0" || nextInput == "1") { "-encryptNativeLibraries 需要 0 或 1" }
            encryptNativeLibraries = nextInput == "1"
        }
        if ("-nativeExclusions" == arg) nativeExclusions = nextInput.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
    ResourceManager.serverBaseUrl = serviceBaseUrl
    ResourceManager.cachePath = cachePath
    println("开始加固")
    val startTime = System.currentTimeMillis()
    JiaGu.deal(
        srcApkPath = srcApkPath,
        secretKey = secretKey,
        keyVi = secretKeyVi,
        androidSdkPath = sdkPath,
        jdkPath = jdkPath,
        signFilePath = signFilePath,
        signAlias = signAlias,
        keyStorePassword = keyStorePassword,
        keyPassword = keyPassword,
        isAndroidX = isAndroidX,
        showLog = isDebug,
        d2cConfigPath = d2cConfigPath,
        obfuscateResourcePaths = obfuscateResourcePaths,
        encryptResourceContents = encryptResourceContents,
        encryptNativeLibraries = encryptNativeLibraries,
        nativeExclusions = nativeExclusions,
    )
    println("加固结束，用时:${(System.currentTimeMillis() - startTime).formatTime()}")
}
