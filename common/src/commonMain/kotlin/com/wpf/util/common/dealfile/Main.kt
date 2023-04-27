package com.wpf.util.common.dealfile

import java.io.File

fun main(args: Array<String>) {
    println(args)
}

fun getChannelFile(channelName: String): File {
    val channelFile = File(channelName)
    if (!channelFile.exists()) {
        channelFile.createNewFile()
    }
    return channelFile
}