package com.wpf.utils

import java.io.File

fun main(args: Array<String>? = null) {
    if (args.isNullOrEmpty()) {
        println("参数异常:请输入参数")
        return
    }
    var filePath = ""
    var findStart = ""
    var findEnd = ""
    var regex = ""
    var replace = ""
    args.forEachIndexed { index, arg ->
        val nextInput = args.getOrNull(index + 1) ?: ""
        when(arg) {
            "-file" -> {
                filePath = nextInput
            }
            "-findStart" -> {
                findStart = nextInput
            }
            "-findEnd" -> {
                findEnd = nextInput
            }
            "-regex" -> {
                regex = nextInput
            }
            "-replace" -> {
                replace = nextInput
            }
        }
    }
    if (regex.isNotEmpty() && replace.isNotEmpty()) {
        FileReplace.findAndReplace(filePath, findStart, findEnd, regex, replace)
    } else {
        val changeFile = File(filePath)
        val fileStr = changeFile.readBytes().decodeToString()
        if (fileStr.isEmpty()) return
        val findStartPos = fileStr.indexOf(findStart)
        val findEndPos = fileStr.indexOf(findEnd)
        val oldReplaceStr = fileStr.substring(findStartPos, findEndPos + findEnd.length)
        println(oldReplaceStr)
    }
}