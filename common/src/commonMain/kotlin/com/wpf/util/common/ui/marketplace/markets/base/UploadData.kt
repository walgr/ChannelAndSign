package com.wpf.util.common.ui.marketplace.markets.base

import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.marketplace.MarketPlaceViewModel

data class UploadData(
    val apk: MarketApk,
    val description: String,
    val imageList: List<String>? = null
)

fun UploadData.upload() {
    MarketPlaceViewModel.getSelectMarket(
        ChannelSetViewModel.getChannelList().find { it.isSelect }?.name ?: "",
        MarketPlaceViewModel.getCanApiMarketList().find { it.name == apk.channelName }?.name ?: ""
    ).query(this)
}

fun UploadData.packageName() = apk.abiApk.getOrNull(0)?.packageName
fun UploadData.versionCode() = apk.abiApk.getOrNull(0)?.versionCode
fun UploadData.versionName() = apk.abiApk.getOrNull(0)?.versionName