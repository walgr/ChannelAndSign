package com.wpf.base.dealfile.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream


object FileUtil {

    fun save2File(inputStream: InputStream, outputFile: File) {
        val out = FileOutputStream(outputFile)
        runCatching {
            var len: Int
            val buf1 = ByteArray(1024)
            while (inputStream.read(buf1).also { len = it } > 0) {
                out.write(buf1, 0, len)
            }
        }.getOrDefault {
            inputStream.close()
            out.close()
        }
    }
}