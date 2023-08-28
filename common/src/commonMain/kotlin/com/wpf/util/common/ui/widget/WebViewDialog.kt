package com.wpf.util.common.ui.widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sun.javafx.scene.web.Debugger
import com.sun.javafx.webkit.WebConsoleListener
import com.wpf.util.webview.*
import io.ktor.http.*
import io.ktor.server.application.*
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import netscape.javascript.JSObject
import java.lang.reflect.Field


@Composable
fun ShowWebView(
    show: MutableState<Boolean>,
    title: String = "",
    url: String,
    cookies: MutableMap<String, List<String>>? = null,
    urlChange: UrlChange? = null,
    dismiss: () -> Unit = {}
) {
    val showDialog = remember { show }

    if (showDialog.value) {
        Window(state = rememberWindowState(
            width = 1280.dp,
            height = 960.dp,
        ), title = title, onCloseRequest = {
            showDialog.value = false
            dismiss.invoke()
        }) {
            WebViewShow(url, cookies, urlChange)
        }
    }
}

@Composable
fun WebViewShow(url: String, cookies: MutableMap<String, List<String>>? = null, urlChange: UrlChange? = null) {
    val browserUrl = rememberWebViewState(url, urlChange = urlChange, cookies = cookies)
    WebView(
        browserUrl,
        modifier = Modifier.fillMaxSize(),
        navigator = rememberWebViewNavigator(),
        {
            engine.isJavaScriptEnabled = true
            cookies?.let {
                it.forEach { (t, u) ->
                    it[t] = u.map { cache ->
                        if (cache.contains("%")) {
                            cache.decodeURLQueryComponent()
                        } else {
                            cache
                        }
                    }
                }
                setCookie(url, cookies)
            }
            runCatching {
                WebConsoleListener.setDefaultListener { _, message, _, _ ->
                    println(message)
                }
                println("当前版本：${engine.userAgent}")
//                CoroutineScope(Dispatchers.IO).launch {
//                    FileServer.start()
//                }
//                val webEngineClazz: Class<*> = WebEngine::class.java
//                val debuggerField: Field = webEngineClazz.getDeclaredField("debugger")
//                debuggerField.setAccessible(true)
//                val debugger: Debugger = debuggerField.get(engine) as Debugger
//                DevToolsDebuggerServer.startDebugServer(debugger, 51742)
//                enableFirebug()
//                engine.loadWorker.stateProperty().addListener { _, _, newState ->
//                    if (newState === Worker.State.SUCCEEDED) {
//                        val javaBridge = JavaBridge()
//                        val window = engine.executeScript("window") as JSObject
//                        window.setMember("console", javaBridge) // "console" object is now known to JavaScript
//                    }
//                }
            }.onFailure {
                it.printStackTrace()
            }
        }) {
        //销毁
        runCatching {
//            FileServer.stop()
//            DevToolsDebuggerServer.stopDebugServer()
        }
    }
}