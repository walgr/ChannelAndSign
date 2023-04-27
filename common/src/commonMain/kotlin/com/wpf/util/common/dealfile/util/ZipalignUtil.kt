package com.wpf.util.common.dealfile.util

import com.wpf.util.common.dealfile.zipalignFile

/**
 * apk对齐
 */

interface Zipalign {
    fun check(inputApkFile: String): Boolean
    fun zipalign(inputApkFile: String, outApkFile: String)
}

object ZipalignUtil : Zipalign {

    override fun check(inputApkFile: String): Boolean {
        return if (zipalignFile.endsWith(".exe")) {
            ZipalignUtilWin.check(inputApkFile)
        } else {
            ZipalignUtilLinux.check(inputApkFile)
        }
    }

    override fun zipalign(inputApkFile: String, outApkFile: String) {
        if (zipalignFile.endsWith(".exe")) {
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
        return resultStr.contains("succesful");
    }

    override fun zipalign(inputApkFile: String, outApkFile: String) {
        val cmd = arrayOf(zipalignFile, "-p", "-f", "4", inputApkFile, outApkFile)
        Runtime.getRuntime().exec(cmd)
    }
}

object ZipalignUtilLinux : Zipalign {

    override fun check(inputApkFile: String): Boolean {
        return false
    }

    override fun zipalign(inputApkFile: String, outApkFile: String) {

    }

}