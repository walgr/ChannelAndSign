package com.wpf.util.common.ui.channelset

import com.russhwolf.settings.set
import com.wpf.base.dealfile.*
import com.wpf.util.common.json
import com.wpf.util.common.settings
import com.wpf.util.common.ui.configset.ConfigPageViewModel
import com.wpf.util.common.ui.signset.SignFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File

object ChannelSetViewModel {

    fun getChannelList(): List<Channel> {
        settings.getStringOrNull("channelList")?.let {
            return json.decodeFromString(it)
        }
        return arrayListOf()
    }

    fun saveChannelList(channelList: List<Channel>) {
        settings["channelList"] = json.encodeToString(channelList)
    }

    fun getChannelDataInFile(txtFilePath: String): List<Array<String>> {
        val channelsFile = File(txtFilePath)
        val result = mutableListOf<Array<String>>()
        if (!channelsFile.exists()) return result
        channelsFile.forEachLine {
            if (it.isNotEmpty()) {
                val fields = it.split(" ")
                val channelApkFileName: String = fields[1].trim().replace("\n", "")
                val channelName: String = fields[2].trim().replace("\n", "")
                result.add(arrayOf(channelApkFileName, channelName))
            }
        }
        return result
    }

    fun dealApk(filePathList: List<String>, channelPath: String?, sign: SignFile, callback: (() -> Unit)) {
        channelsFilePath = channelPath ?: ""
        channelBaseInsertFilePath = ConfigPageViewModel.getChannelBaseFilePath()
        channelSavePath = ConfigPageViewModel.getChannelSaveFilePath()
        zipalignFile = ConfigPageViewModel.getZipalignFilePath()
        signFile = sign.StoreFile
        signPassword = sign.StorePass
        signAlias = sign.KeyAlias
        signAliasPassword = sign.KeyPass
        var returnTime = 0
        filePathList.forEach {
            ChannelAndSign.scanFile(it) {
                returnTime++
                if (returnTime == filePathList.size) {
                    //运行结束
                    callback.invoke()
                }
            }
        }
    }
}