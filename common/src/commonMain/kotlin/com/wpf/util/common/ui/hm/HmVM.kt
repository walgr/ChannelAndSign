package com.wpf.util.common.ui.hm

import com.wpf.util.common.ui.http.client
import com.wpf.util.common.ui.utils.settings
import com.wpf.utils.ex.md5
import com.wpf.utils.http.HttpClient
import com.wpf.utils.tools.HDCUtil
import io.ktor.client.request.head
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.File
import java.sql.Time
import java.util.Timer
import java.util.TimerTask

class ServerHapInfo(val hapFile: File = null!!) {
    var uploadServerTime: Long = 0
}

object HmVM {

    private val downloadFilePath = File(arrayOf(File(".").canonicalPath, "temp", "hmHap").joinToString(File.separator))
    private var curDownloadFile: File? = null
    private var curDownloadFileMD5: String = ""
        get() {
            if (field.isEmpty()) {
                field = settings.getString("hmCurDownloadFileMD5", "")
            }
            return field
        }
        set(value) {
            field = value
            settings.putString("hmCurDownloadFileMD5", value)
        }
    private var curDownloadFileUploadServerTime: Long = 0
        get() {
            if (field == 0L) {
                field = settings.getLong("hmCurDownloadFileUploadServerTime", 0)
            }
            return field
        }
        set(value) {
            field = value
            settings.putLong("hmCurDownloadFileUploadServerTime", value)
        }

    private var curDownloadJob: Job? = null

    fun clearDownloadCache() {
        downloadFilePath.deleteRecursively()
        curDownloadFile = null
        curDownloadFileMD5 = ""
        curDownloadFileUploadServerTime = 0L
        curDownloadJob?.cancel()
    }

    fun getClientList() = HDCUtil.getClientList()

    fun getDownloadFile(): ServerHapInfo? {
        if (downloadFilePath.exists() && (downloadFilePath.listFiles()?.size ?: 0) > 0) {
            val lastFile = downloadFilePath.listFiles()!!.last()
            if (lastFile.md5() == curDownloadFileMD5) {
                return ServerHapInfo(lastFile)
            }
        }
        return null
    }

    fun getFileInDownloadPathByMd5(md5: String): File? {
        if (downloadFilePath.exists() && (downloadFilePath.listFiles()?.size ?: 0) > 0) {
            return downloadFilePath.listFiles()!!.findLast { it.md5() == md5 }
        }
        return null
    }

    fun getNewHmHap(
        serverBaseUrl: String,
        packageName: String,
        appVersion: String,
        forceDownload: Boolean = false,
        callback: (file: ServerHapInfo) -> Unit
    ) {
        val getNewHmHapUrl = "${serverBaseUrl}/getHap?package=$packageName&appVersion=$appVersion"
        runBlocking {
            println("检查是否有新包:${getNewHmHapUrl}")
            val headers = client.head(getNewHmHapUrl).headers
            val fileMd5 = headers[HttpHeaders.ContentDisposition]?.split(";")?.find {
                it.contains("md5=")
            }?.substringAfterLast("md5=") ?: ""
            val fileUploadServerTime = (headers[HttpHeaders.ContentDisposition]?.split(";")?.find {
                it.contains("uploadTime=")
            }?.substringAfterLast("uploadTime=") ?: "0").toLong()
            val downloadFileFindByMd5 = getFileInDownloadPathByMd5(fileMd5)
            if (downloadFileFindByMd5 != null) {
                curDownloadFile = downloadFileFindByMd5
                curDownloadFileMD5 = fileMd5
                curDownloadFileUploadServerTime = fileUploadServerTime
                callback(ServerHapInfo(downloadFileFindByMd5).apply {
                    uploadServerTime = fileUploadServerTime
                })
                println("本地已下载新包:${curDownloadFile!!.path}")
                return@runBlocking
            }
            if (curDownloadFileMD5 == fileMd5) {
                return@runBlocking
            }
            if (forceDownload) {
                curDownloadJob?.cancel()
            } else {
                if (curDownloadJob != null) {
                    return@runBlocking
                }
            }
            println("有新包，当前正在下载:${getNewHmHapUrl}")
            curDownloadJob = HttpClient.downloadFile(getNewHmHapUrl, savePath = downloadFilePath.path) {
                if (it != null) {
                    curDownloadFile = it
                    curDownloadFileMD5 = fileMd5
                    curDownloadFileUploadServerTime = fileUploadServerTime
                    callback(ServerHapInfo(curDownloadFile!!).apply {
                        uploadServerTime = fileUploadServerTime
                    })
                    println("新版本下载完毕")
                } else {
                    curDownloadFile = null
                    curDownloadFileMD5 = ""
                    curDownloadFileMD5 = ""
                    curDownloadFileUploadServerTime = 0L
                }
            }
        }
    }

    fun setDownloadData(
        serverBaseUrl: String,
        packageName: String,
        appVersion: String,
        callback: (file: ServerHapInfo) -> Unit
    ) {
        if (serverBaseUrl.isNotEmpty() && packageName.isNotEmpty() && appVersion.isNotEmpty()) {
            timingGetNewHap(
                serverBaseUrl = serverBaseUrl,
                packageName = packageName,
                appVersion = appVersion,
                callback = callback,
            )
        }
    }

    private var timer: Timer? = null
    fun timingGetNewHap(
        time: Long = 30000L,
        serverBaseUrl: String,
        packageName: String,
        appVersion: String,
        forceDownload: Boolean = false,
        callback: (file: ServerHapInfo) -> Unit
    ) {
        if (timer != null) {
            return
        }
//        timer?.cancel()
        timer = Timer()
        println("开始定时获取新包，间隔:${time}")
        timer?.schedule(object : TimerTask() {
            override fun run() {
                getNewHmHap(serverBaseUrl, packageName, appVersion, forceDownload, callback)
            }
        }, time)
    }
}