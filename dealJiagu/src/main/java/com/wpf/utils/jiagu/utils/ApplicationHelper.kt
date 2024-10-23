package com.wpf.utils.jiagu.utils

import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.createCheck
import com.wpf.utils.tools.ManifestEditorUtil
import net.dongliu.apk.parser.ApkParsers
import java.io.File
import java.io.InputStream

object ApplicationHelper {

    fun getPackageName(srcApkFile: File): String? {
        val srcManifestFileStr = ApkParsers.getManifestXml(srcApkFile)
        val applicationStr = "(?<=<manifest)(.*?)(?=>)".toRegex().find(srcManifestFileStr)!!.value
        return "(?<=package=\")(.*?)(?=\")".toRegex().find(applicationStr)?.value
    }

    /**
     * 带包名
     */
    fun getName(srcApkFile: File): String? {
        val srcManifestFileStr = ApkParsers.getManifestXml(srcApkFile)
        val applicationStr = "(?<=<application)(.*?)(?=>)".toRegex().find(srcManifestFileStr)!!.value
        return "(?<=android:name=\")(.*?)(?=\")".toRegex().find(applicationStr)?.value
    }

    fun setNewName(curPath: String, androidManifestIS: InputStream, newName: String): File {
        val androidManifest = "AndroidManifest.xml"
        val srcManifestFile = File(curPath + File.separator + "AndroidManifest_src.xml").createCheck(true)
        FileUtil.save2File(androidManifestIS, srcManifestFile)
        androidManifestIS.close()
        val fixManifestFile = File(curPath + File.separator + androidManifest).createCheck(true)
        ManifestEditorUtil.doCommand(
            mutableListOf(
                srcManifestFile.path,
                "-f",
                "-o",
                fixManifestFile.path,
                "-an",
                newName
            )
        )
        srcManifestFile.delete()
        return fixManifestFile
    }
}