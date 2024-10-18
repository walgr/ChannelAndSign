package com.wpf.utils.tools

import com.wpf.utils.ResourceManager
import com.wpf.utils.ex.FileUtil
import com.wpf.utils.isLinuxRuntime
import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime
import java.io.File

object JADXUtil {

    private var jadxOutPath: String = ""
    private val jadxPath: String = ""
        get() {
            if (field.isEmpty() || !File(field).exists()) {
                val jadxZipFile = ResourceManager.getResourceFile("jadx-1.4.7.zip", isFile = true)
                jadxOutPath = jadxZipFile.parent + File.separator + "jadx-1.4.7" + File.separator
                val outFile = File(jadxOutPath)
                if (!outFile.exists() || outFile.listFiles()?.isEmpty() != false) {
                    FileUtil.unZipFiles(jadxZipFile, jadxOutPath)
                }
                return jadxOutPath + "bin" + File.separator + if (isWinRuntime) "jadx.bat" else "jadx"
            } else {
                return field
            }
        }

    fun delJar() {
        ResourceManager.delResourceByPath("jadx-1.4.7")
    }

    /**
     * jadx-1.4.7/bin/jadx -d "/mnt/d/Android/Android Project/AutoDebug" --single-class "cn.goodjobs.community.SophixStubApplication" classes.dex
     */
    fun getJavaInDex(findJavaFile: String, outJavaPath: String, dexPath: String): Boolean {
        if (isLinuxRuntime || isMacRuntime) {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "777", jadxPath)).waitFor()
        }
        val cmd = arrayOf(jadxPath, "-d", outJavaPath, "--single-class", findJavaFile, dexPath)
        val process = Runtime.getRuntime().exec(cmd)
        LogStreamThread(process.inputStream, false) {
            !it.contains("Saving class")
        }.start()
        val result = process.waitFor()
        process.destroy()
        return result == 0
    }
}