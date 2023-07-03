package com.wpf.util.common.ui.marketplace.markets

data class UploadData(
    val apk: MarketApk,
    val description: String,
    val imageList: List<String>? = null
)


fun UploadData.upload() {

}