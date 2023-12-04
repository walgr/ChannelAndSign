package com.wpf.base.dealfile.util

import com.wpf.base.dealfile.curPath
import java.io.File

object ResourceManager {

    fun getTempPath() = curPath + "temp" + File.separator

    fun getResourceFile(name: String, outPath: String = "", overwrite: Boolean = false, isFile: Boolean = true): File {
        val outFile =
            File(outPath.ifEmpty { getTempPath() + name })
        if (outFile.exists() && !overwrite) return outFile
        outFile.createCheck(isFile)
        val resourceIS = FileUtil.javaClass.getResourceAsStream("/$name")
        val outIS = outFile.outputStream()
        resourceIS?.copyTo(outIS)
        resourceIS?.close()
        outIS.close()
        if (outFile.length() == 0L) {
            println("导出文件失败,源文件大小:${resourceIS?.readBytes()?.size}")
        }
        return outFile
    }

    fun delResourceByName(name: String) {
        File(getTempPath() + name).deleteRecursively()
    }

    fun delResourceByPath(path: String) {
        File(path).deleteRecursively()
    }

    fun delTemp() {
        File(curPath + "temp").deleteRecursively()
    }
}