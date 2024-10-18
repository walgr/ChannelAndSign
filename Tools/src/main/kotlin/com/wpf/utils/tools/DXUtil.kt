package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

object DXUtil {

    private val dxPath: String = ""
        get() {
            if (field.isEmpty() || !File(field).exists()) {
                val jadxZipFile = ResourceManager.getResourceFile("dx.zip", isFile = true)
                val dxOutPath = jadxZipFile.parent + File.separator + "dx"
                val outFile = File(dxOutPath)
                if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                    FileUtil.unZipFiles(jadxZipFile, dxOutPath)
                }
                outFile.delete()
                return dxOutPath + File.separator + if (isWinRuntime) "dx.bat" else "dx"
            } else {
                return field
            }
        }

    fun delJar() {
        ResourceManager.delResourceByPath("dx")
    }

    fun jar2Dex(jarFilePath: String, dexFilePath: String = ""): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", dxPath)).waitFor()
        }
        val cmd = arrayOf(dxPath, "--dex", "--output", dexFilePath, jarFilePath)
        val process = Runtime.getRuntime().exec(cmd)
        LogStreamThread(process.inputStream).start()
        LogStreamThread(process.errorStream).start()
        val result = process.waitFor()
        process.destroy()
        return result == 0
    }
}