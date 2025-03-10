package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

class HMClientInfo {
    val name: String = ""
    val connectInfo: String = ""
}

object HDCUtil {
    private val toolPath: String = ""
        get() {
            if (field.isEmpty() || !File(field).exists()) {
                val zipFile = ResourceManager.getResourceFile("tools/hdc/win/hdc.zip", isFile = true)
                val outPath = zipFile.parent + File.separator + "hdc"
                val outFile = File(outPath)
                if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                    FileUtil.unZipFiles(zipFile, outPath)
                }
                outFile.delete()
                return outPath + File.separator + if (isWinRuntime) "hdc.exe" else "hdc"
            } else {
                return field
            }
        }

    fun dealCommand(commands: MutableList<String>): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", toolPath)).waitFor()
        }
        commands.add(0, toolPath)
        val process = Runtime.getRuntime().exec(commands.toTypedArray())
        LogStreamThread(process.inputStream).start()
        LogStreamThread(process.errorStream).start()
        val result = process.waitFor()
        process.destroy()
        return result == 0
    }

    fun getClientList(): List<HMClientInfo> {
        dealCommand(mutableListOf("list", "targets", "-v"))
        return emptyList()
    }
}