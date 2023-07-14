package com.wpf.util.common.ui.marketplace.markets

import com.wpf.util.common.ui.marketplace.markets.base.Market
import kotlinx.serialization.Serializable

@Serializable
data class ChannelMarket(
    val channelName: String,
    val marketList: List<Market>,
)