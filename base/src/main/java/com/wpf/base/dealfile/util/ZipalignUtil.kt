package com.wpf.base.dealfile.util

import com.wpf.base.dealfile.isLinuxRuntime
import com.wpf.base.dealfile.isMacRuntime
import com.wpf.base.dealfile.isWinRuntime
import java.io.File

/**
 * apk对齐
 */

object ZipalignUtil {
    private val zipalignPath: String by lazy {
        if (isWinRuntime) {
            return@lazy ResourceManager.getResourceFile("zipalign.exe", isFile = true).path
        } else if (isLinuxRuntime) {
            val zipalignZipFile = ResourceManager.getResourceFile("zipalign_linux.zip", isFile = true)
            val zipalignOutPath = zipalignZipFile.parent + File.separator + "zipalign_linux"
            FileUtil.unZipFiles(zipalignZipFile, zipalignOutPath)
            zipalignZipFile.delete()
            return@lazy zipalignOutPath + File.separator + "zipalign_linux"
        } else if (isMacRuntime) {
            return@lazy ResourceManager.getResourceFile("zipalign_mac", isFile = true).path
        } else {
            ""
        }
    }

    fun delJar() {
        ResourceManager.delResourceByPath(zipalignPath)
        ResourceManager.delResourceByPath(ResourceManager.getTempPath() + File.separator + "zipalign_linux")
    }

    fun check(inputApkFile: String): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "775", zipalignPath)).waitFor()
        }
        val cmd = arrayOf(zipalignPath, "-c", "-v", "4", inputApkFile)
        val result = Runtime.getRuntime().exec(cmd, null, File(zipalignPath).parentFile)
        val resultStr = result.inputStream.readBytes().decodeToString()
        result.waitFor()
        result.destroy()
        return resultStr.contains("succesful")
    }

    fun zipalign(inputApkFile: String, outApkFile: String): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "775", zipalignPath)).waitFor()
        }
        val cmd = arrayOf(zipalignPath, "-p", "-f", "-v", "4", inputApkFile, outApkFile)
        val result = Runtime.getRuntime().exec(cmd, null, File(zipalignPath).parentFile)
        val resultStr = result.inputStream.readBytes().decodeToString()
        val error = result.errorStream.readBytes().decodeToString()
        if (error.isNotEmpty()) {
            println(error)
        }
        if (!resultStr.contains("succesful")) {
            println(resultStr)
        }
        result.waitFor()
        result.destroy()
        return resultStr.contains("succesful")
    }
}