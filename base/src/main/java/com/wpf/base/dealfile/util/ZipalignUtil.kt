package com.wpf.base.dealfile.util

import com.wpf.base.dealfile.zipalignFile

/**
 * apk对齐
 */

interface Zipalign {
    fun check(inputApkFile: String): Boolean
    fun zipalign(inputApkFile: String, outApkFile: String): Boolean
}

object ZipalignUtil : Zipalign {

    override fun check(inputApkFile: String): Boolean {
        return if (zipalignFile.endsWith(".exe")) {
            ZipalignUtilWin.check(inputApkFile)
        } else {
            ZipalignUtilLinux.check(inputApkFile)
        }
    }

    override fun zipalign(inputApkFile: String, outApkFile: String): Boolean {
        return if (zipalignFile.endsWith(".exe")) {
            ZipalignUtilWin.zipalign(inputApkFile, outApkFile)
        } else {
            ZipalignUtilLinux.zipalign(inputApkFile, outApkFile)
        }
    }
}

object ZipalignUtilWin : Zipalign {

    override fun check(inputApkFile: String): Boolean {
        val cmd = arrayOf(zipalignFile, "-c", "-v", "4", inputApkFile)
        val result = Runtime.getRuntime().exec(cmd)
        val resultStr = result.inputStream.readBytes().decodeToString()
        return resultStr.contains("succesful")
    }

    override fun zipalign(inputApkFile: String, outApkFile: String): Boolean {
        val cmd = arrayOf(zipalignFile, "-p", "-f", "4", inputApkFile, outApkFile)
        val result = Runtime.getRuntime().exec(cmd)
        val resultStr = result.inputStream.readBytes().decodeToString()
        val error = result.errorStream.readBytes().decodeToString()
        if (error.isNotEmpty()) {
            println(error)
        }
        result.destroy()
        return resultStr.contains("succesful")
    }
}

object ZipalignUtilLinux : Zipalign {

    override fun check(inputApkFile: String): Boolean {
        val shell = arrayOf(zipalignFile, "-c", "-v", "4", inputApkFile)
        val result = Runtime.getRuntime().exec(shell)
        val resultStr = result.inputStream.readBytes().decodeToString()
        return resultStr.contains("succesful")
    }

    override fun zipalign(inputApkFile: String, outApkFile: String): Boolean {
        val shell = arrayOf(zipalignFile, "-p", "-f", "4", inputApkFile, outApkFile)
        val result = Runtime.getRuntime().exec(shell)
        val resultStr = result.inputStream.readBytes().decodeToString()
        val error = result.errorStream.readBytes().decodeToString()
        if (error.isNotEmpty()) {
            println(error)
        }
        result.destroy()
        return resultStr.contains("succesful")
    }

}