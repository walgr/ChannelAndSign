package com.wpf.util.common.ui.marketplace.markets.base

import com.wpf.util.common.ui.marketplace.markets.*
import kotlin.reflect.KClass

/**
 * 支持平台
 */
enum class MarketType(val channelName: String, val url: String = "", val market: KClass<out Market> = UnknownMarket::class) {
    `360`("360", "https://i.360.cn/login/?src=pcw_open_app&destUrl=https%3A%2F%2Fdev.360.cn%3A443%2Fmod4%2Fmobilenavs%2Findex"),
    百度("Baidu", "https://app.baidu.com/newapp/index"),
    应用宝("QQ", "https://open.qq.com/login"),
    魅族("Meizu", "https://login.flyme.cn/"),
    三星("Samsung", market = SamsungMarket::class),
    小米("Xiaomi", market = XiaomiMarket::class),
    华为("Huawei", market = HuaweiMarket::class),
    Oppo("Oppo", market = OppoMarket::class),
    Vivo("Vivo", market = VivoMarket::class),
    未知("Unknown", ""),
    ;

    fun canApi(): Boolean {
        return market != UnknownMarket::class
    }
}

object MarketTypeHelper {
    fun find(channelName: String): MarketType? {
        return MarketType.values().find {
            it.channelName == channelName
        }
    }
}
