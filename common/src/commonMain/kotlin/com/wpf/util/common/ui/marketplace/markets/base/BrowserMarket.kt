package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import com.wpf.util.common.ui.widget.ShowWebView
import javafx.scene.web.WebView

interface BrowserMarket : Market {

    override val baseUrl: String
        get() = ""

    val browserUrl: String

    val showBrowserS: MutableState<Boolean>

    val cookies: MutableMap<String, String>

    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        val showBrowser = remember { showBrowserS }
        Button(onClick = {
            showBrowser.value = true
        }) {
            Text("获取Token")
        }
        ShowWebView(showBrowserS, url = browserUrl, cookies = cookies, urlChange = ::onWebUrlChange)
    }

    fun onWebUrlChange(url: String?, webView: WebView) {

    }

}