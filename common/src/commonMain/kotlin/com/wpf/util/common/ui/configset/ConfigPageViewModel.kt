package com.wpf.util.common.ui.configset

import com.wpf.util.common.ui.utils.settings

object ConfigPageViewModel {
    fun getChannelBaseFilePath() = settings.getString("channelBaseFilePath", "")

    fun getChannelSaveFilePath() = settings.getString("channelSaveFilePath", "")

    fun getZipalignFilePath() = settings.getString("zipalignFilePath", "")
}