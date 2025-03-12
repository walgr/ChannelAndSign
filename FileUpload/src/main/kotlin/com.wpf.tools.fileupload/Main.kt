package com.wpf.tools.fileupload

import java.io.File

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常，请检查输入")
        return
    }
    var serviceBaseUrl = "http://0.0.0.0:8080/"
    var uploadFilePath = ""
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        if (arg.startsWith("-") && nextInput.startsWith("-")) {
            println("参数异常，请检查输入${arg.replace("-", "")}")
            return
        }
        when(arg) {
            "-serviceBaseUrl" -> {
                serviceBaseUrl = nextInput
            }
            "-uploadFilePath" -> {
                uploadFilePath = nextInput
            }
        }
    }
    val uploadFile = File(uploadFilePath)
    if (!uploadFile.exists() || uploadFile.length() == 0L) {
        println("文件路径错误:${uploadFilePath}")
        return
    }
    if (HMUpload.filterExtension.contains(uploadFile.extension)) {
        HMUpload.upload(serviceBaseUrl, uploadFile)
    }
    if (ApkUpload.filterExtension.contains(uploadFile.extension)) {
        ApkUpload.upload(serviceBaseUrl, uploadFile)
    }
}