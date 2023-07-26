package com.wpf.util.common.ui.marketplace.markets

import CookieManagerCompat
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.BrowserMarket
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.utils.Callback
import javafx.scene.web.WebView
import kotlinx.serialization.Transient
import java.net.URI

class QH360Market : BrowserMarket {

    override val name: String = MarketType.qh360.channelName
    override var isSelect: Boolean = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    @Transient
    override val baseUrl: String = ""
    @Transient
    override val browserUrl: String = "https://dev.360.cn/mod3/mobilenavs/index"
    @Transient
    override val showBrowserS = mutableStateOf(false)
    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {

    }

    override val cookies: MutableMap<String, String> = mutableMapOf()
    override fun onWebUrlChange(url: String?, webView: WebView) {
        super.onWebUrlChange(url, webView)
        if (url.isNullOrEmpty()) return
        println(url)
        if (url.startsWith("https://dev.360.cn/mod3/mobilenavs/index?_=")) {
            //已经登录成功
            val urlCookies = CookieManagerCompat.getCookie(URI.create(url))
            urlCookies?.let {
                cookies.clear()
                cookies.putAll(it)
            }
            println("url:${url}----Cookie:${cookies}")
        }

    }
}