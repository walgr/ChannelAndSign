package com.wpf.util.common.ui.marketplace.markets.base

import com.wpf.util.common.ui.marketplace.markets.*
import kotlin.reflect.KClass

/**
 * 支持平台
 */
enum class MarketType(val channelName: String, val market: KClass<out Market> = UnknownMarket::class) {
    qh360("360", market = QH360Market::class),
    百度("Baidu"),
    应用宝("QQ"),
    魅族("Meizu"),
    三星("Samsung", market = SamsungMarket::class),
    小米("Xiaomi", market = XiaomiMarket::class),
    华为("Huawei", market = HuaweiMarket::class),
    Oppo("Oppo", market = OppoMarket::class),
    Vivo("Vivo", market = VivoMarket::class),
    未知("Unknown"),
    ;

    fun isApi(): Boolean {
        return market != UnknownMarket::class
    }

    fun isBrowser() = market is BrowserMarket
}

object MarketTypeHelper {
    fun find(channelName: String): MarketType? {
        return MarketType.values().find {
            it.channelName == channelName
        }
    }
}

