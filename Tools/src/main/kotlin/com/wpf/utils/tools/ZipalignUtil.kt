package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

/**
 * apk对齐
 */

object ZipalignUtil {
    private val zipalignPath: String = ""
        get() {
            if (field.isEmpty() || !File(field).exists()) {
                return if (isWinRuntime) {
                    ResourceManager.getResourceFile("zipalign/zipalign.exe", isFile = true).path
                } else if (isLinuxRuntime) {
                    val zipalignZipFile = ResourceManager.getResourceFile("zipalign/zipalign_linux.zip", isFile = true)
                    val zipalignOutPath = zipalignZipFile.parent + File.separator + "zipalign_linux"
                    val outFile = File(zipalignOutPath)
                    if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                        FileUtil.unZipFiles(zipalignZipFile, zipalignOutPath)
                    }
                    zipalignOutPath + File.separator + "zipalign_linux"
                } else if (isMacRuntime) {
                    ResourceManager.getResourceFile("zipalign/zipalign_mac", isFile = true).path
                } else {
                    ""
                }
            } else {
                return field
            }
        }

    fun delJar() {
        ResourceManager.delResourceByPath(zipalignPath)
        ResourceManager.delResourceByPath("zipalign_linux")
    }

    fun check(inputApkFile: String): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "+x", zipalignPath)).waitFor()
        }
        val cmd = arrayOf(zipalignPath, "-c", "-v", "4", inputApkFile)
        val result = Runtime.getRuntime().exec(cmd)
        val resultStr = result.inputStream.readBytes().decodeToString()
        result.waitFor()
        result.destroy()
        return resultStr.contains("succesful")
    }

    fun zipalign(inputApkFile: String, outApkFile: String, outRenameToInput: Boolean = false): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "+x", zipalignPath)).waitFor()
        }
        val cmd = arrayOf(zipalignPath, "-p", "-f", "-v", "4", inputApkFile, outApkFile)
        val result = Runtime.getRuntime().exec(cmd)
        val resultStr = result.inputStream.readBytes().decodeToString()
        LogStreamThread(result.errorStream).start()
        if (!resultStr.contains("succesful")) {
            println(resultStr)
        }
        result.waitFor()
        result.destroy()
        if (outRenameToInput) {
            val inputFile = File(inputApkFile)
            inputFile.delete()
            File(outApkFile).renameTo(inputFile)
        }
        return resultStr.contains("succesful")
    }
}