package com.wpf.util.common.ui.widget

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.sun.javafx.scene.web.Debugger
import com.wpf.server.FileServer
import com.wpf.util.webview.*
import io.ktor.http.*
import io.ktor.server.application.*
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import netscape.javascript.JSObject
import java.lang.reflect.Field
import kotlin.concurrent.thread


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
    var browserUrl = rememberWebViewState(url, urlChange = urlChange, cookies = cookies)
//    browserUrl = rememberWebViewStateWithHTMLData(
//        "<form method=\"post\" enctype=\"multipart/form-data\">\n" +
//                "  <div>\n" +
//                "    <label for=\"profile_pic\">选择要上传的文件</label>\n" +
//                "    <input\n" +
//                "      type=\"file\"\n" +
//                "      id=\"profile_pic\"\n" +
//                "      name=\"profile_pic\"\n" +
//                "      accept=\".jpg, .jpeg, .png\" />\n" +
//                "  </div>\n" +
//                "  <div>\n" +
//                "    <button>提交</button>\n" +
//                "  </div>\n" +
//                "</form>\n", null, urlChange = urlChange
//    )
    System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    WebView(
        browserUrl,
        modifier = Modifier.fillMaxSize(),
        navigator = rememberWebViewNavigator(),
        {
            engine.isJavaScriptEnabled = true
            cookies?.let {
                setCookie(url, cookies)
            }
            runCatching {
                CoroutineScope(Dispatchers.IO).launch {
                    FileServer.start()
                }
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
            FileServer.stop()
//            DevToolsDebuggerServer.stopDebugServer()
        }
    }
}