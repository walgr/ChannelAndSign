package com.wpf.util.common.ui.marketplace

import java.io.File

data class UploadData(
    val apk: MarketApk,
    val description: String,
    val imageList: List<String>? = null
)


fun UploadData.upload() {

}