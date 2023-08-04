package com.wpf.util.webview

import java.net.CookieHandler
import java.net.URI
import java.util.LinkedHashMap

actual object CookieManagerCompat {

    actual fun setCookie(url: String,  headers: MutableMap<String, String>) {
        val uri: URI = URI.create(url)
        var stringList: List<String> = headers.map {
            "${it.key}=${it.value}"
        }
        if (headers.size == 1 && headers.containsKey("Cookie")) {
            stringList = mutableListOf("Cookie=" + headers["Cookie"])
        }
        val headersCookie: MutableMap<String, List<String>> = LinkedHashMap()
        headersCookie["Set-Cookie"] = stringList
        CookieHandler.getDefault().put(uri, headersCookie)
    }

    actual fun setCookie(url: String, cookies: List<String>) {
        val uri: URI = URI.create(url)
        val headersCookie: MutableMap<String, List<String>> = LinkedHashMap()
        headersCookie["Set-Cookie"] = cookies
        CookieHandler.getDefault().put(uri, headersCookie)
    }

    actual fun getCookie(uri: URI): MutableMap<String, String>? {
        return CookieHandler.getDefault().get(uri, java.util.HashMap()).map {
            it.key to it.value[0]
        }.toMap().toMutableMap()
    }

}