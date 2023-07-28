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

    override var canPush: Boolean = false

    @Transient
    val pushUrl = "https://dev.360.cn/mod3/createmobile/baseinfo?id="

    @Transient
    override val showBrowserS = mutableStateOf(false)

    override fun uploadAbi() = arrayOf(AbiType.Abi32_64)

    override fun push(uploadData: UploadData, callback: Callback<MarketType>) {
        if (uploadData.packageName().isNullOrEmpty() || !isUploadUrl) return
        //上传apk
        //设置更新文案
        webView?.setElementValue("edition_brief", uploadData.description)
    }

    @Transient
    override val cookies: MutableMap<String, String> = getLocalCookies()

    private fun getLocalCookies(): MutableMap<String, String> {
        val cookieJson = settings.getString("${name}Cookie", "{}")
        return mapGson.fromJson(
            cookieJson,
            object : TypeToken<MutableMap<String, String>>() {}.type
        )
    }

    private fun saveCookies() {
        settings.putString("${name}Cookie", mapGson.toJson(cookies))
    }

    private fun clearCookies() {
        settings.putString("${name}Cookie", "{}")
    }

    @Transient
    private var qid = ""
        get() = settings.getString("${name}Qid", "")
        set(value) {
            field = value
            settings.putString("${name}Qid", value)
        }

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
            val realUrl = webView.querySelector(" minimalize-styl-2", "href")
            if (realUrl.isNullOrEmpty() || realUrl == "undefined") {
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
                    saveCookies()
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
                saveCookies()
            }
            webView.load(pushUrl + appId)
        } else if (url.startsWith(pushUrl)) {
            //上传页面
            isUploadUrl = true

            webView?.setElementValue("edition_brief", "testdaweawe")
        }
    }

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
            cookies = cookies,
            urlChange = ::onWebUrlChange
        )
    }

    override fun initByData() {
        super.initByData()
    }

    override fun clearInitData() {
        super.clearInitData()
        clearCookies()
        qid = ""
        cookies.clear()
    }
}