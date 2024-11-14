package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

object SophixPatchTool : SophixPatchBaseTool() {

    var showLog = false
    private val sophixPatchRunPath: String = ""
        get() {
            return if (field.isEmpty() || !File(field).exists()) {
                if (isWinRuntime) {
                    val sophixPatchZipFile =
                        ResourceManager.getResourceFile("SophixPatchTool/SophixPatchTool_windows.zip")
                    val outFile =
                        File(sophixPatchZipFile.parent + File.separator + "SophixPatchTool_windows/SophixPatchTool-3.2.8")
                    if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                        FileUtil.unZipFiles(sophixPatchZipFile, sophixPatchZipFile.parent)
                    }
                    sophixPatchZipFile.parent + File.separator +
                            "SophixPatchTool_windows/SophixPatchTool-3.2.8/".replace(
                                "/",
                                File.separator
                            ) + "SophixPatchTool.exe"
                } else if (isLinuxRuntime) {
                    val sophixPatchZipFile =
                        ResourceManager.getResourceFile("SophixPatchTool/SophixPatchTool_linux.zip")
                    val outFile = File(sophixPatchZipFile.parent + File.separator + "SophixPatchTool-3.2.5")
                    if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                        FileUtil.unZipFiles(sophixPatchZipFile, sophixPatchZipFile.parent)
                    }
                    sophixPatchZipFile.parent + File.separator +
                            "SophixPatchTool-3.2.5/".replace(
                                "/",
                                File.separator
                            ) + "SophixPatchTool"
                } else if (isMacRuntime) {
                    val sophixPatchZipFile =
                        ResourceManager.getResourceFile("SophixPatchTool/SophixPatchTool_mac.zip")
                    val outFile = File(sophixPatchZipFile.parent + File.separator + "SophixPatchTool-1.0.0")
                    if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                        FileUtil.unZipFiles(sophixPatchZipFile, sophixPatchZipFile.parent)
                    }
                    sophixPatchZipFile.parent + File.separator +
                            "SophixPatchTool/Contents/MacOS/".replace(
                                "/",
                                File.separator
                            ) + "SophixPatchTool"
                } else ""
            } else {
                field
            }
        }

    var process: Process? = null
    override fun deal(configPath: String): Boolean {
        runCatching {
            if (sophixPatchRunPath.isEmpty()) return false
            if (isLinuxRuntime || isMacRuntime) {
                Runtime.getRuntime().exec(arrayOf("chmod", "-R", "+x", sophixPatchRunPath)).waitFor()
            }
            val sophixPatchRunFile = File(sophixPatchRunPath)
            val cmd = if (isWinRuntime)
                mutableListOf("cmd", "/c", "SophixPatchTool", "--profiles", configPath)
            else mutableListOf("./SophixPatchTool", "--profiles", configPath)
            val processB =
                ProcessBuilder(
                    *cmd.toTypedArray(),
                ).directory(sophixPatchRunFile.parentFile)
            if (showLog) {
                processB.inheritIO()
            }
            process = processB.start()
            LogStreamThread(process!!.inputStream, showLog).start()
            LogStreamThread(process!!.errorStream, showLog).start()
            process!!.waitFor()
            process!!.destroy()
            return true
        }.getOrElse {
            println("运行SophixPatchTool失败:${it.message}")
            if (it.message?.contains("CreateProcess error=740") == true) {
                println("请用管理员权限运行")
            }
        }
        return false
    }
}