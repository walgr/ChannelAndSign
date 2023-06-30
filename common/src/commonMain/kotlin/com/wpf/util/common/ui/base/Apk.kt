package com.wpf.util.common.ui.base

import com.wpf.util.common.ui.marketplace.MarketType
import com.wpf.util.common.ui.marketplace.markets.Market
import com.wpf.util.common.ui.utils.abiType
import com.wpf.util.common.ui.utils.channelName
import kotlinx.serialization.Serializable
import test.AXMLPrinter

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
    var versionName: String = "1.0.0",
    var versionCode: String = "1",
    var abi: AbiType = AbiType.Abi32_64,
    var channelName: String = "",
) {
    init {
        channelName = name.channelName() ?: ""
        abi = name.abiType()
        val apkXmlStr = AXMLPrinter.getManifestXMLFromAPK(filePath).replace("\n", "")
        if (apkXmlStr.isNotEmpty()) {
            val versionNameStrS = "android:versionName=\""
            val versionNameStrE = "\" "
            val versionNameIndexS = apkXmlStr.indexOf(versionNameStrS)
            val versionNameIndexE = apkXmlStr.indexOf(versionNameStrE, versionNameIndexS + versionNameStrS.length)
            versionName = apkXmlStr.subSequence(versionNameIndexS + versionNameStrS.length, versionNameIndexE).trim().toString()
            val versionCodeStrS = "android:versionCode=\""
            val versionCodeStrE = "\" "
            val versionCodeIndexS = apkXmlStr.indexOf(versionCodeStrS)
            val versionCodeIndexE = apkXmlStr.indexOf(versionCodeStrE, versionCodeIndexS + versionCodeStrS.length)
            versionCode = apkXmlStr.subSequence(versionCodeIndexS + versionCodeStrS.length, versionCodeIndexE).trim().toString()
        }
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