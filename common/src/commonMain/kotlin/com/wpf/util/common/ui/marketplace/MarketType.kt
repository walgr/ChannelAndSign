package com.wpf.util.common.ui.marketplace

import com.wpf.util.common.ui.marketplace.markets.HuaweiMarket
import com.wpf.util.common.ui.marketplace.markets.Market
import com.wpf.util.common.ui.marketplace.markets.VivoMarket
import com.wpf.util.common.ui.marketplace.markets.XiaomiMarket

/**
 * 支持平台
 */
enum class MarketType(val channelName: String, val url: String, val canApi: Boolean = false, val market: Market? = null) {
    `360`("360", "https://i.360.cn/login/?src=pcw_open_app&destUrl=https%3A%2F%2Fdev.360.cn%3A443%2Fmod4%2Fmobilenavs%2Findex"),
    百度("Baidu", "https://app.baidu.com/newapp/index"),
    应用宝("QQ", "https://open.qq.com/login"),
    三星("Samsung", "https://account.samsung.cn/accounts/v1/MBR/signInGate?locale=zh_CN&countryCode=CN&goBackURL=https:%2F%2Faccount.samsung.cn%2Fmembership%2Fintro&returnURL=https:%2F%2Faccount.samsung.cn%2Fmembership%2Fintro&redirect_uri=https:%2F%2Faccount.samsung.cn%2Fmbr-svc%2Fauth%2FregistAuthentication&tokenType=OAUTH&response_type=code&client_id=ea2r064y73&state=nIWTsCGrOrBrzUKzTXaigLTuQcLEapnZ"),
    小米("Xiaomi", "https://dev.mi.com/distribute", true, XiaomiMarket),
    华为("Huawei", "https://id1.cloud.huawei.com/AMW/portal/home.html", true, HuaweiMarket),
    Oppo("Oppo", "https://open.oppomobile.com/new/loginForHeyTap?location=https%3A%2F%2Fopen.oppomobile.com%2F"),
    Vivo("Vivo", "https://id.vivo.com.cn/#/user/login", true, VivoMarket),
    魅族("Meizu", "https://login.flyme.cn/"),
    未知("Unknown", ""),
}