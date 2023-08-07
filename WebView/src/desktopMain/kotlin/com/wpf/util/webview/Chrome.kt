//package com.wpf.util.webview
//
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.awt.SwingPanel
//import javafx.application.Platform
//import javafx.embed.swing.JFXPanel
//import javafx.scene.Scene
//import javafx.scene.layout.StackPane
//import javafx.scene.web.WebView
//import me.friwi.jcefmaven.CefAppBuilder
//import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
//import me.friwi.jcefmaven.impl.progress.ConsoleProgressHandler
//import org.cef.CefApp
//import org.cef.CefClient
//import org.cef.browser.CefBrowser
//import java.awt.Component
//import java.io.File
//
//object Chrome {
//
//    private fun getApp(): CefApp {
//        //Create a new CefAppBuilder instance
//        val builder = CefAppBuilder()
//
//        //Configure the builder instance
//        builder.setInstallDir(File("jcef-bundle")) //Default
//        builder.setProgressHandler(ConsoleProgressHandler()) //Default
//        builder.addJcefArgs("--disable-gpu"); //Just an example
//        builder.cefSettings.windowless_rendering_enabled = true; //Default - select OSR mode
//
//        //Set an app handler. Do not use CefApp.addAppHandler(...), it will break your code on MacOSX!
//        builder.setAppHandler(object : MavenCefAppHandlerAdapter() {
//
//        });
//
//        //Build a CefApp instance using the configuration above
//        return builder.build()
//    }
//
//    fun getClient(): CefClient = getApp().createClient()
//    fun getBrowser(url: String, isOffscreenRendered: Boolean = false, isTransparent: Boolean = false): CefBrowser =
//        getClient().createBrowser(url, isOffscreenRendered, isTransparent)
//}
//
//@Composable
//internal fun ChromeDesktop(
//    state: WebViewState,
//    modifier: Modifier = Modifier,
//    navigator: WebViewNavigator = rememberWebViewNavigator(),
//    onCreated: CefClient.() -> Unit = {},
//    onDispose: CefClient.() -> Unit = {},
//) {
//    var webView by remember { mutableStateOf<CefClient?>(null) }
//
//    LaunchedEffect(webView, navigator) {
//        with(navigator) {  }
//    }
//
//    val currentOnDispose by rememberUpdatedState(onDispose)
//
//    webView?.let {
//        DisposableEffect(it) {
//            onDispose { currentOnDispose(it) }
//        }
//    }
//
//    SwingPanel(factory = {
//        JFXPanel().also { jfxP ->
//            //解决第二次打开白屏问题
//            Platform.setImplicitExit(false)
//            Platform.runLater {
//                webView = Chrome.getClient()
//                val rootVewView = Chrome.getBrowser(state.content.getCurrentUrl()!!).uiComponent
//                val root = StackPane()
////                root.children.add(rootVewView)
//                val scene = Scene(root)
////                onCreated.invoke(rootVewView)
////                addEngineListener(rootVewView, state, navigator)
//                when (val content = state.content) {
//                    is WebContent.Url -> {
////                        val url = content.url
////
////                        if (url.isNotEmpty() && url != rootVewView.getCurrentUrl()) {
////                            rootVewView.load(url)
////                        }
//                    }
//
//                    is WebContent.Data -> {
////                        rootVewView.loadContent(content.data)
//                    }
//                }
//                jfxP.scene = scene
//            }
//        }
//    }, modifier = modifier)
//}