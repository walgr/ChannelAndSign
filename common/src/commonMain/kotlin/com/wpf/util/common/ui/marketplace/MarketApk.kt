package com.wpf.util.common.ui.marketplace

import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.base.SelectItem

data class MarketApk(
    val marketType: MarketType,
    val channelName: String,
    val abiApk: MutableList<Apk>
) : SelectItem() {
    override var isSelect: Boolean = marketType.canApi
}