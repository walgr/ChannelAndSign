package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

class HMClientInfo {
    var name: String = ""
    var connectInfo: String = ""
}

object HDCUtil {
    private val toolPath: String = ""
        get() {
            if (field.isEmpty() || !File(field).exists()) {
                val zipFile = ResourceManager.getResourceFile("tools/hdc/hdc.zip", isFile = true)
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

    fun dealCommand(commands: MutableList<String>, logCallback: ((log: String) -> Unit)? = null): String {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", toolPath)).waitFor()
        }
        commands.add(0, toolPath)
        val process = Runtime.getRuntime().exec(commands.toTypedArray())
        var logs = ""
        LogStreamThread(process.inputStream, true, showAllLog = {
            logs = it
            logCallback?.invoke(logs)
            false
        }).start()
        LogStreamThread(process.errorStream).start()
        if (logCallback == null) process.waitFor()
        process.destroy()
        return logs
    }

    fun getClientList(): List<HMClientInfo> {
        val clientsLog = dealCommand(mutableListOf("list", "targets"))
        val clientsLogs = clientsLog.split("\n").filter { it.isNotEmpty() }
        if (clientsLogs.size == 1 && clientsLogs[0] == "[Empty]") return emptyList()
        return clientsLogs.map {
            HMClientInfo().apply {
                name = it.split(" ")[0]
                connectInfo = it
            }
        }
    }

    fun connectClient(clientAddress: String, callbackInSuccess: () -> Unit) {
        println("开始连接鸿蒙设备:$clientAddress")
        var isSuccess = false
        dealCommand(mutableListOf("tconn", clientAddress)) { connectLog ->
            println(connectLog)
            isSuccess = connectLog.contains("OK")
            println("连接鸿蒙设备:${if (isSuccess) "成功" else "失败"}")
            if (isSuccess) {
                callbackInSuccess()
            }
        }
    }

    fun installHap(hapFile: File, appBundleId: String, abilityName: String, connectKey: String = ""): Boolean {
        println("开始安装到鸿蒙设备" + (if (connectKey.isEmpty()) "" else ":$connectKey"))
        var isSuccess = false
        println(dealCommand(mutableListOf("shell", "aa", "force-stop", appBundleId).apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        }))
        val tmpDir = (appBundleId + System.currentTimeMillis()).hashCode()
        println(dealCommand(mutableListOf("shell", "mkdir", "data/local/tmp/${tmpDir}").apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        }))
        println(dealCommand(mutableListOf("file", "send", hapFile.path, "data/local/tmp/${tmpDir}").apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        }))
        val installLog = dealCommand(mutableListOf("shell", "bm", "install", "-p", "data/local/tmp/${tmpDir}").apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        })
        println(installLog)
        isSuccess = installLog.contains("successfully")
        println(dealCommand(mutableListOf("shell", "rm", "-rf", "data/local/tmp/${tmpDir}").apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        }))
        println(dealCommand(mutableListOf("shell", "aa", "start", "-a", abilityName, "-b", appBundleId).apply {
            if (connectKey.isNotEmpty()) {
                addAll(0, listOf("-t", connectKey))
            }
        }))
        println("安装到鸿蒙设备${connectKey}:${if (isSuccess) "成功" else "失败"}")
        return isSuccess
    }

    fun changeToUsb(): Boolean {
        println("开始切换")
        var isSuccess = false
        val installLog = dealCommand(mutableListOf("tmode", "usb"))
        println(installLog)
        isSuccess = installLog.contains("successful")
        println("切换:${if (isSuccess) "成功" else "失败"}")
        return isSuccess
    }

    fun changeToPort(port: String = "5555"): Boolean {
        println("开始切换")
        var isSuccess = false
        val installLog = dealCommand(mutableListOf("tmode", "port", port))
        println(installLog)
        isSuccess = installLog.contains("successful")
        println("切换:${if (isSuccess) "成功" else "失败"}")
        return isSuccess
    }
}