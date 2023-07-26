package com.wpf.util.common.ui.widget

import CookieManagerCompat
import UrlChange
import WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import rememberWebViewNavigator
import rememberWebViewState

@Composable
fun ShowWebView(
    show: MutableState<Boolean>,
    title: String = "",
    url: String,
    cookies: MutableMap<String, String>? = null,
    urlChange: UrlChange? = null,
    dismiss: () -> Unit = {}
) {
    val showDialog = remember { show }

    if (showDialog.value) {
        Dialog(state = rememberDialogState(
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
fun WebViewShow(url: String, cookies: MutableMap<String, String>?= null, urlChange: UrlChange? = null) {
    val browserUrl = rememberWebViewState(url, urlChange = urlChange)
    WebView(
        browserUrl,
        modifier = Modifier.fillMaxSize(),
        navigator = rememberWebViewNavigator(),
        {
            engine.isJavaScriptEnabled = true
            cookies?.let {
                CookieManagerCompat.setCookie(url, cookies)
            }
        }) {
    }
}