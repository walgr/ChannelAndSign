package com.wpf.utils.ex

import org.apache.commons.codec.digest.DigestUtils
import java.io.File

fun File.createCheck(isFile: Boolean = false, data: ByteArray? = null): File {
    if (!exists()) {
        if (parentFile?.exists() != true) {
            parentFile?.mkdirs()
        }
        if (isFile) {
            if (!exists()) {
                createNewFile()
            }
        } else {
            mkdir()
        }
    }
    data?.let {
        writeBytes(it)
    }
    return this
}

fun File.getChildFileList(): List<File>? {
    if (!exists()) return null
    if (isFile) return listOf(this)
    val childFileList = mutableListOf<File>()
    listFiles()?.forEach {
        it.getChildFileList()?.let { childList ->
            childFileList.addAll(childList)
        }
    }
    return childFileList
}

fun File.md5(): String {
    return DigestUtils.md5Hex(inputStream())
}

fun File.delCheck() {
    if (exists()) {
        deleteRecursively()
    }
}