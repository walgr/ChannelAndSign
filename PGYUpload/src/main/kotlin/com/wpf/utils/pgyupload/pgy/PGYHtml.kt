package com.wpf.utils.pgyupload.pgy

import com.wpf.utils.pgyupload.pgy.data.ApkInfo
import java.io.File
import java.nio.charset.Charset

object PGYHtml {

    fun deal(curPath: String, isTest: String, host: String, release: String, baseHtmlPath: String, apkInfo: ApkInfo) {
        val baseHtmlFile = File(baseHtmlPath)
        if (!baseHtmlFile.exists()) {
            println("基础Html未找到")
            return
        }
        var baseHtmlStr = baseHtmlFile.readText(Charset.forName("utf-8"))
        val pgyHtmlFile = File(curPath + File.separator + "pgy.html")
        if (!pgyHtmlFile.exists()) {
            pgyHtmlFile.createNewFile()
        }
        baseHtmlFile.copyTo(pgyHtmlFile, true)
        baseHtmlStr = baseHtmlStr.replace("\${appQRCodeURL}", apkInfo.buildQRCodeURL)
        baseHtmlStr = baseHtmlStr.replace("\${buildVersion}", apkInfo.buildVersion)
        baseHtmlStr = baseHtmlStr.replace("\${buildUpdated}", apkInfo.buildUpdated)
        baseHtmlStr = baseHtmlStr.replace("\${host}", host)
        baseHtmlStr = baseHtmlStr.replace("\${test}", isTest)
        baseHtmlStr = baseHtmlStr.replace("\${release}", release)
        pgyHtmlFile.writeText(baseHtmlStr, Charset.forName("utf-8"))
    }
}