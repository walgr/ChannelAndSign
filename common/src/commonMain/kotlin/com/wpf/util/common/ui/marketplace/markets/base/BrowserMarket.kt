package com.wpf.util.common.ui.marketplace.markets.base

import androidx.compose.runtime.MutableState
import javafx.scene.web.WebView

interface BrowserMarket : Market {

    override val baseUrl: String
        get() = ""

    val browserUrl: String

    val showBrowserS: MutableState<Boolean>

    val cookies: MutableMap<String, String>

    val canPush: Boolean

    fun onWebUrlChange(url: String?, webView: WebView) {

    }

    fun WebView.querySelector(selector: String, attribute: String): String? {
        return this.engine.executeScript("document.querySelector('a[class=\\'$selector\\']')?.$attribute")?.toString()
    }

    fun WebView.setElementValue(name: String, value: String) {
        if (findElements(name)) {
            this.engine.executeScript("document.getElementsByName('${name}').item(0).value = '${value}'")
        }
    }

    fun WebView.setElementCheck(name: String, value: Boolean) {
        if (findElements(name)) {
            this.engine.executeScript("document.getElementsByName('${name}').item(0).checked = '${value}'")
        }
    }

    fun WebView.findElements(name: String): Boolean {
        val returnObj = this.engine.executeScript("document.getElementsByName('${name}')?.length")
        return returnObj != "undefined" && (returnObj != 0 || returnObj != "0")
    }
    fun WebView.buttonSubmit(name: String) {
        this.engine.executeScript("\$('.$name').submit();")
    }

}