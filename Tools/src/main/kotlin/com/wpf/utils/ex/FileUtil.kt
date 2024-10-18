package com.wpf.utils.ex

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream


object FileUtil {

    fun save2File(inputStream: InputStream, outputFile: File) {
        val out = FileOutputStream(outputFile)
        runCatching {
            var len: Int
            val buf1 = ByteArray(1024)
            while (inputStream.read(buf1).also { len = it } > 0) {
                out.write(buf1, 0, len)
            }
            inputStream.close()
            out.close()
        }
    }

    /**
     * 解压文件
     *
     * @param zipFile：需要解压缩的文件
     * @param descDir：解压后的目标目录
     */
    @Throws(IOException::class)
    fun unZipFiles(zipFile: File, descDir: String) {
        // 读入流
        val zipInputStream = ZipInputStream(FileInputStream(zipFile))
        // 遍历每一个文件
        var zipEntry = zipInputStream.nextEntry
        while (zipEntry != null) {
            if (zipEntry.isDirectory) { // 文件夹
                val unzipFilePath: String = descDir + File.separator + zipEntry.name
                // 直接创建
                mkdir(File(unzipFilePath))
            } else { // 文件
                val unzipFilePath: String = descDir + File.separator + zipEntry.name
                val file = File(unzipFilePath)
                // 创建父目录
                mkdir(file.parentFile)
                // 写出文件流
                val bufferedOutputStream = BufferedOutputStream(FileOutputStream(unzipFilePath))
                val bytes = ByteArray(1024)
                var readLen: Int
                while (zipInputStream.read(bytes).also { readLen = it } != -1) {
                    bufferedOutputStream.write(bytes, 0, readLen)
                }
                bufferedOutputStream.close()
            }
            zipInputStream.closeEntry()
            zipEntry = zipInputStream.nextEntry
        }
        zipInputStream.close()
    }

    // 如果父目录不存在则创建
    private fun mkdir(file: File?) {
        if (null == file || file.exists()) {
            return
        }
        mkdir(file.parentFile)
        file.mkdir()
    }
}