package com.wpf.util.common.ui.base

import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.utils.abiType
import com.wpf.util.common.ui.utils.channelName
import kotlinx.serialization.Serializable
import net.dongliu.apk.parser.ApkFile

enum class AbiType(val type: String) {
    Abi32("armeabi-v7a"),
    Abi64("arm64-v8a"),
    Abi32_64("universal"),
}
@Serializable
data class Apk(
    val name: String,
    val size: Long,
    val filePath: String,
    var appIcon: String? = null,
    var appName: String = "",
    var packageName: String = "",
    var versionName: String = "1.0.0",
    var versionCode: String = "1",
    var abi: AbiType = AbiType.Abi32_64,
    var channelName: String = "",
) {
    init {
        channelName = name.channelName() ?: ""
        abi = name.abiType()
        val apkInfo = ApkFile(filePath)
        val apkMeta = apkInfo.apkMeta
        appName = apkMeta.label
        packageName = apkMeta.packageName
        versionName = apkMeta.versionName
        versionCode = apkMeta.versionCode.toString()
    }
}


fun Apk.canApi(): Boolean {
    return MarketType.values().find { market ->
        channelName.contains(market.channelName, ignoreCase = true)
    }?.canApi ?: false
}

fun Apk.getMarket(): Market? {
    return MarketType.values().find { market ->
        channelName.contains(market.channelName, ignoreCase = true)
    }?.market
}