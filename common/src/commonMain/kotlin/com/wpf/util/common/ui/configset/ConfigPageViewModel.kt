package com.wpf.util.common.ui.configset

import com.russhwolf.settings.set
import com.wpf.util.common.settings

object ConfigPageViewModel {
    fun getChannelBaseFilePath() = settings.getString("channelBaseFilePath", "")

    fun saveChannelBaseFilePath(path: String) {
        settings["channelBaseFilePath"] = path
    }

    fun getChannelSaveFilePath() = settings.getString("channelSaveFilePath", "")

    fun saveChannelSaveFilePath(path: String) {
        settings["channelSaveFilePath"] = path
    }

    fun getZipalignFilePath() = settings.getString("zipalignFilePath", "")

    fun saveZipalignFilePath(path: String) {
        settings["zipalignFilePath"] = path
    }
}