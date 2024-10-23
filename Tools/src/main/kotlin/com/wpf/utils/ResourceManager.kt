package com.wpf.utils

import com.wpf.utils.ex.checkWinPath
import com.wpf.utils.ex.createCheck
import com.wpf.utils.http.CacheFile
import kotlinx.coroutines.runBlocking
import java.io.File

object ResourceManager {

    fun getTempPath() = curPath + File.separator + "temp" + File.separator
    var serverBaseUrl = "http://0.0.0.0:8080/"
    var cachePath = ""
        get() {
            return field.ifEmpty { rootPath }
        }

    fun getResourceFile(
        resource: String,
        outPath: String = "",
        overwrite: Boolean = false,
        isHttp: Boolean = true,
        isFile: Boolean = true
    ): File {
        val resourceIS = javaClass.getResourceAsStream("/$resource")
        if (isHttp && resourceIS == null) {
            return runBlocking {
                CacheFile.downloadFileSuspend(
                    "${serverBaseUrl}getResources?name=$resource",
                    outFilePath = outPath.ifEmpty { cachePath + File.separator + "cache" + File.separator + resource.checkWinPath() })!!
            }
        } else {
            val outFile = File(outPath.ifEmpty { getTempPath() + resource })
            if (outFile.exists() && !overwrite) return outFile
            outFile.createCheck(isFile)
            val outIS = outFile.outputStream()
            resourceIS?.copyTo(outIS)
            resourceIS?.close()
            outIS.close()
            if (outFile.length() == 0L) {
                println("导出文件失败,源文件大小:0")
            }
            return outFile
        }
    }

    fun delResourceByName(name: String) {
        File(curPath + "temp" + File.separator + name).deleteRecursively()
    }

    fun delResourceByPath(path: String) {
        if (path.contains("temp")) {
            File(path).deleteRecursively()
        }
    }

    fun delTemp() {
        File(getTempPath()).deleteRecursively()
    }
}