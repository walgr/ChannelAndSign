package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.marketplace.MarketPlaceViewModel

data class UploadData(
    val apk: MarketApk,
    val description: String,
    val imageList: List<String>? = null
)


fun UploadData.upload() {
    MarketPlaceViewModel.getMarketSaveList().find {
        it.name == apk.marketType.market.name
    }?.query(this)
}