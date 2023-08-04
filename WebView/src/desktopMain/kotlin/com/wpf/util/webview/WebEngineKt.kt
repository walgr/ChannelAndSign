package com.wpf.util.webview

import java.net.CookieHandler
import java.net.URI
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import kotlin.collections.HashMap



fun WebView.load(url: String) {
    engine.load(url)
}

fun WebView.loadContent(content: String) {
    engine.loadContent(content)
}

fun WebView.stopLoading() {
    engine.stopLoading()
}

fun WebView.goForward() {
    engine.goForward()
}

fun WebView.goBack() {
    engine.goBack()
}

fun WebView.canGoBack(): Boolean {
    return engine.canGoBack()
}

fun WebView.canGoForward(): Boolean {
    return engine.canGoForward()
}


fun WebView.getCurrentUrl(): String? {
    return engine.getCurrentUrl()
}

fun WebView.goRoot() {
    engine.history.go(-engine.history.currentIndex)
}

fun WebView.executeScript(script: String) {
    engine.executeScript(script)
}


/**
 * Set cookie
 * 需要在加载之前设置，才有效果
 * It needs to be set before loading to have an effect
 * @param url
 * @param headers
 */
fun WebView.setCookie(url: String, headers: MutableMap<String, List<String>>) {
    val uri: URI = URI.create(url)
    CookieHandler.getDefault().put(uri, headers)
}


fun WebView.getCookies(): MutableMap<String, MutableList<String>>? {
    val cookieHandler = CookieHandler.getDefault()
    val uri = URI.create(engine.location)
    return cookieHandler.get(uri, HashMap())
}


internal fun WebEngine.getCurrentUrl(): String? {
    if (history.entries.size <= 0) return null
    return history.entries[history.currentIndex].url
}

internal fun WebEngine.stopLoading() {
    loadWorker.cancel()
}

internal fun WebEngine.goForward() {
    if (canGoForward()) {
        history.go(1)
    }
}

internal fun WebEngine.goBack() {
    if (canGoBack()) {
        history.go(-1)
    }
}

internal fun WebEngine.canGoBack(): Boolean {
    return history.maxSize > 0 && history.currentIndex != 0
}

internal fun WebEngine.canGoForward(): Boolean {
    return history.maxSize > 0 && history.currentIndex != history.maxSize - 1
}




