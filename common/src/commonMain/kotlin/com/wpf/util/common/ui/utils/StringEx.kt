package com.wpf.util.common.ui.utils

import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.utils.isWinRuntime
import java.io.File

fun String.marketType(): MarketType {
    return MarketType.entries.find { market ->
        this.contains(market.channelName, ignoreCase = true)
    } ?: MarketType.未知
}
fun String.channelName(): String? {
    return MarketType.entries.find { market ->
        this.contains(market.channelName, ignoreCase = true)
    }?.channelName
}

fun String.abiType(): AbiType {
    return AbiType.entries.find { abi ->
        this.contains(abi.type)
    } ?: AbiType.Abi32_64
}

fun String?.asFile() = if (this == null) null else File(this)

fun String.checkWinPath(): String {
    if (isWinRuntime) {
        return replace("/", "\\\\").replace("%20", " ")
    }
    return this
}