package com.wpf.util.common.ui.marketplace.markets

import CookieManagerCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.gson.reflect.TypeToken
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.*
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.ShowWebView
import com.wpf.util.common.ui.widget.common.InputView
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import load
import java.net.URI

data class QH360Market(
    var account: String = "",
    var password: String = "",
    var appId: String = "",
) : BrowserMarket {

    override val name: String = MarketType.qh360.channelName
    override var isSelect: Boolean = false

    @Transient
    override val isSelectState: MutableState<Boolean> = mutableStateOf(isSelect)

    @Transient
    override val baseUrl: String = ""

    @Transient
    override val browserUrl: String = "https://dev.360.cn/mod3/mobilenavs/index"

    //        get() = if (qid.isEmpty()) field else "$field?qid=$qid"
    @Transient
    override var canPush: Boolean = false

    @Transient
    val pushUrl = "https://dev.360.cn/mod3/createmobile/baseinfo?id="

    @Transient
    override val showBrowserS = mutableStateOf(false)

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

//    @Transient
//    private var packageName = remember { mutableStateOf("") }
    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is QH360Market) return
        val showBrowser = remember { showBrowserS }
        val account = remember { mutableStateOf(market.account) }
        account.value = market.account
        val password = remember { mutableStateOf(market.password) }
        password.value = market.password
        val appId = remember { mutableStateOf(market.appId) }
        appId.value = market.appId
        Column {
            InputView(input = account, hint = "请配置账号") {
                account.value = it
                market.account = it
            }
            InputView(input = password, hint = "请配置密码") {
                password.value = it
                market.password = it
            }
            InputView(input = appId, hint = "请配置AppId") {
                appId.value = it
                market.appId = it
            }
            Button(onClick = {
                showBrowser.value = true
            }) {
                Text("获取Token")
            }
        }
        ShowWebView(
            showBrowser,
            url = browserUrl,
            cookies = cookies.value,
            urlChange = ::onWebUrlChange
        )
    }

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.packageName().isNullOrEmpty() || !isUploadUrl) return
        //上传apk
        //设置更新文案
        webView?.setElementValue("edition_brief", uploadData.description)
    }

    @Transient
    val cookies = autoSaveMap("${name}Cookie") { mutableMapOf<String, String>() }

    @delegate:Transient
    private var qid by autoSave("${name}Qid") { "" }

    @Transient
    private var isUploadUrl = false

    @Transient
    private var webView: WebView? = null
    override fun onWebUrlChange(url: String?, webView: WebView) {
        super.onWebUrlChange(url, webView)
        if (url.isNullOrEmpty()) return
        this.webView = webView
        println(url)
        isUploadUrl = false
        if (url == browserUrl) {
            //初始打开
            if (webView.findElements("userName")) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    webView.setElementValue("userName", account)
                    webView.setElementValue("password", password)
                    webView.setElementCheck("is_agree", true)
                    webView.buttonSubmit("quc-button-primary")
                }
            } else {
                val urlCookies = CookieManagerCompat.getCookie(URI.create(url))
                urlCookies?.let {
                    cookies.clear()
                    cookies.putAll(it)
                }
                webView.load(pushUrl + appId)
            }
        }
        if (url.startsWith("https://dev.360.cn/mod3/mobilenavs/index?qid=")) {
            //已经登录成功
            val qidStr = url.substringAfter("https://dev.360.cn/mod3/mobilenavs/index?qid=")
            val urlCookies = CookieManagerCompat.getCookie(URI.create(url))
            if (qidStr != qid) {
                qid = qidStr
            }
            urlCookies?.let {
                cookies.clear()
                cookies.putAll(it)
            }
            webView.load(pushUrl + appId)
        } else if (url.startsWith(pushUrl + appId)) {
            //上传页面
            isUploadUrl = true

            webView?.setElementValue("edition_brief", "testdaweawe")
            webView?.setElementValue("apk_desc", "testdaweawe")
        }
    }

    override fun initByData() {
        super.initByData()
    }

    override fun clearCache() {
        super.clearCache()
        qid = ""
        cookies.clear()
    }
}