package com.wpf.utils.jiagu.utils

import com.wpf.utils.ex.FileUtil
import com.wpf.utils.ex.createCheck
import com.wpf.utils.tools.ManifestEditorUtil
import net.dongliu.apk.parser.parser.BinaryXmlParser
import net.dongliu.apk.parser.parser.XmlTranslator
import java.io.File
import java.io.InputStream

object ApplicationHelper {

    // Package, split and application names are literal manifest attributes; no resource table is required.
    fun manifestXml(apk: File): String = java.util.zip.ZipFile(apk).use { zip ->
        val entry = requireNotNull(zip.getEntry("AndroidManifest.xml")) { "APK 缺少 AndroidManifest.xml" }
        val bytes = zip.getInputStream(entry).use { it.readBytes() }
        val translator = XmlTranslator()
        BinaryXmlParser(java.nio.ByteBuffer.wrap(bytes), null).apply {
            xmlStreamer = translator
            parse()
        }
        translator.xml
    }

    fun getPackageName(srcApkFile: File): String? {
        val srcManifestFileStr = manifestXml(srcApkFile)
        val applicationStr = "(?<=<manifest)(.*?)(?=>)".toRegex().find(srcManifestFileStr)!!.value
        return "(?<=package=\")(.*?)(?=\")".toRegex().find(applicationStr)?.value
    }

    /**
     * 带包名
     */
    fun getName(srcApkFile: File): String? {
        val srcManifestFileStr = manifestXml(srcApkFile)
        val applicationStr = "(?<=<application)(.*?)(?=>)".toRegex().find(srcManifestFileStr)!!.value
        val name = "(?<=android:name=\")(.*?)(?=\")".toRegex().find(applicationStr)?.value
            ?.takeIf { it.isNotBlank() } ?: return "android.app.Application"
        val packageName = getPackageName(srcApkFile).orEmpty()
        return when {
            name.startsWith(".") -> packageName + name
            !name.contains('.') -> "$packageName.$name"
            else -> name
        }
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
