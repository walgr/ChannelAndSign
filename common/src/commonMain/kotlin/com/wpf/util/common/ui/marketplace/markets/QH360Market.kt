package com.wpf.util.common.ui.marketplace.markets

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import com.wpf.server.serverBasePath
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.marketplace.markets.base.*
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.ShowWebView
import com.wpf.util.common.ui.widget.common.InputView
import com.wpf.util.webview.CookieManagerCompat
import com.wpf.util.webview.getCookies
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.wpf.util.webview.load
import org.openqa.selenium.Cookie
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File
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

    @Transient
    var webDriver: ChromeDriver? = null

    @delegate:Transient
    var cookieList by autoSaveSet("qh360Cookie") { mutableSetOf<Cookie>() }

    //    @Transient
//    private var packageName = remember { mutableStateOf("") }
    @Composable
    override fun dispositionViewInBox(market: Market) {
        super.dispositionViewInBox(market)
        if (market !is QH360Market) return
        var showBrowser by remember { showBrowserS }
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
                showBrowser = true
            }) {
                Text("获取Token")
            }
        }

        if (showBrowser) {
//            showBrowserS.value = false
//            showBrowser = false
//            runCatching {
//                if (webDriver == null) {
//                    webDriver = ChromeDriver()
//                    webDriver?.manage()?.window()?.size = Dimension(1280, 960)
//                }
//                webDriver?.get(browserUrl)
////                webDriver?.manage()?.deleteAllCookies()
////                cookieList?.forEach {
////                    webDriver?.manage()?.addCookie(it)
////                }
//                webDriver?.manage()?.timeouts()?.implicitlyWait(Duration.ofMillis(5000))
//                WebDriverWait(webDriver!!, Duration.ofSeconds(3)).until {
//                    it?.findElements(By.name("userName")) != null
//                }
//                webDriver?.findElements(By.name("userName"))?.getOrNull(0)
//                    ?.sendKeys("D:\\企业上传\\市场图\\1080x1920\\ad1x.jpg")
//                webDriver?.findElements(By.name("userName"))?.getOrNull(0)?.sendKeys(market.account)
//                webDriver?.findElements(By.name("password"))?.getOrNull(0)?.sendKeys(market.password)
//                webDriver?.findElements(By.name("is_agree"))?.getOrNull(0)?.click()
//                webDriver?.findElements(By.className("quc-button-primary"))?.getOrNull(0)?.submit()
//
//                webDriver?.manage()?.cookies?.let {
//                    cookieList.value = it
//                    cookieList.saveData()
//                }
//
////                webDriver?.get(pushUrl + market.appId)
//                WebDriverWait(webDriver!!, Duration.ofSeconds(3)).until {
//                    webDriver?.findElements(By.name("file"))?.getOrNull(0) != null
//                }
//
//            }.getOrDefault {
//
//            }
        } else {
//            webDriver?.quit()
        }
        ShowWebView(
            showBrowserS,
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
    val cookies = autoSaveMap("${name}Cookie") { mutableMapOf<String, List<String>>() }

    @delegate:Transient
    private var qid by autoSave("${name}Qid") { "" }

    @Transient
    private var isUploadUrl = false

    @Transient
    private var webView: WebView? = null
    override fun onWebUrlChange(url: String?, webView: WebView) {
        super.onWebUrlChange(url, webView)
//        webView?.inputFile("file", File("gradlew"))
        val file = File("D:\\企业上传\\市场图\\1080x1920\\ad1x.jpg")
        if (url.isNullOrEmpty()) return
        this.webView = webView
        println(url)
        isUploadUrl = false
        if (url == browserUrl) {
            //初始打开
            val realUrl = webView.querySelector(" minimalize-styl-2", "href")
            if (webView.findElements("userName") && (realUrl.isNullOrEmpty() || realUrl == "undefined")) {
                CoroutineScope(Dispatchers.Main).launch {
                    delay(1000)
                    webView.setElementValue("userName", account)
                    webView.setElementValue("password", password)
                    webView.setElementCheck("is_agree", true)
                    webView.buttonSubmit("quc-button-primary")
                }
            } else {
                webView.getCookies()?.let {
                    cookies.clear()
                    cookies.putAll(it)
                }
                webView.load(pushUrl + appId)
            }
        }
        if (url.startsWith("https://dev.360.cn/mod3/mobilenavs/index?qid=")) {
            //已经登录成功
            val qidStr = url.substringAfter("https://dev.360.cn/mod3/mobilenavs/index?qid=")
            if (qidStr != qid) {
                qid = qidStr
            }
//            webView.load(pushUrl + appId)
        } else if (url.startsWith(pushUrl + appId) || url == "https://dev.360.cn/mod3/mobileapp/?qid=160270285&appid=201882426") {
            //上传页面
            isUploadUrl = true
            CoroutineScope(Dispatchers.Main).launch {
                delay(3000)
                webView?.setElementValue("edition_brief", "testdaweawe")
                webView?.setElementValue("apk_desc", "testdaweawe")
                webView?.inputFile("file", 7, filePath = getFileLocalUrl("ad1x.jpg"), "ad1x.jpg")
            }
        }
    }

    override fun initByData() {
        super.initByData()
        serverBasePath = settings.getString("serverBasePath", "")
    }

    override fun clearCache() {
        super.clearCache()
        qid = ""
        cookies.clear()
    }
}