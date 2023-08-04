package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.runtime.mutableStateOf
import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.base.SelectItem

data class MarketApk(
    val marketType: MarketType,
    val channelName: String,
    val abiApk: MutableList<Apk> = mutableListOf()
) : SelectItem() {
    override var isSelect: Boolean = marketType.isApi()

    val uploadState = mutableStateOf(UploadState.UPLOAD_WAIT)               //上传状态

    fun packageName() = abiApk[0].packageName

    fun versionCode() = abiApk[0].versionCode

    fun versionName() = abiApk[0].versionName
}