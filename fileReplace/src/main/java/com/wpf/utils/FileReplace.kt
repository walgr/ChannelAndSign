package com.wpf.utils

import java.io.File

object FileReplace {

    fun findAndReplace(filePath: String, findStart: String = "", findEnd: String = "", regex: String, replace: String) {
        if (filePath.isEmpty()) {
            println("文件路径参数异常")
            return
        }
        val changeFile = File(filePath)
        val fileStr = changeFile.readBytes().decodeToString()
        if (fileStr.isEmpty()) return
        val newFileStr = if (findStart.isNotEmpty()) {
            val findStartPos = fileStr.indexOf(findStart)
            val findEndPos = fileStr.indexOf(findEnd)
            val oldReplaceStr = fileStr.substring(findStartPos, findEndPos + findEnd.length)
            val newReplaceStr = oldReplaceStr.replace(regex.toRegex(), replace)
            fileStr.replaceFirst(oldReplaceStr, newReplaceStr)
        } else {
            fileStr.replace(regex.toRegex(), replace)
        }
        changeFile.writeText(newFileStr)
    }
}