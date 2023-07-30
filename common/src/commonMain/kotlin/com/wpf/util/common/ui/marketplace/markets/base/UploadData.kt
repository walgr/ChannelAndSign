package com.wpf.util.common.ui.marketplace.markets.base

import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.marketplace.MarketPlaceViewModel
import com.wpf.util.common.ui.utils.Callback

data class UploadData(
    val apk: MarketApk,
    val description: String,  //更新文案
    val leaveMessage: String?, //留言
    val imageList: List<String>? = null
)

fun UploadData.upload() {
    apk.uploadState.value = UploadState.UPLOADING
    MarketPlaceViewModel.getSelectMarket(
        ChannelSetViewModel.getClientList().find { it.isSelect }?.name ?: "",
        MarketPlaceViewModel.getCanApiMarketList().find { it.name == apk.channelName }?.name ?: ""
    )?.push(this, object : Callback<MarketType> {
        override fun onSuccess(t: MarketType) {
            apk.uploadState.value = UploadState.UPLOAD_SUCCESS
            apk.changeSelect(false)
        }

        override fun onFail(msg: String) {
            apk.uploadState.value = UploadState.UPLOAD_FAIL
        }

    })
}

fun UploadData.packageName() = apk.abiApk.getOrNull(0)?.packageName
fun UploadData.versionCode() = apk.abiApk.getOrNull(0)?.versionCode
fun UploadData.versionName() = apk.abiApk.getOrNull(0)?.versionName