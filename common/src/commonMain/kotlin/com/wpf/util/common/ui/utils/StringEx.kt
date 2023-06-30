package com.wpf.util.common.ui.utils

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.MarketType

fun String.marketType(): MarketType {
    return MarketType.values().find { market ->
        this.contains(market.channelName, ignoreCase = true)
    } ?: MarketType.未知
}
fun String.channelName(): String? {
    return MarketType.values().find { market ->
        this.contains(market.channelName, ignoreCase = true)
    }?.channelName
}

fun String.abiType(): AbiType {
    return AbiType.values().find { abi ->
        this.contains(abi.type)
    } ?: AbiType.Abi32_64
}