package com.wpf.util.webview

class JavaBridge {
    fun log(text: String?) {
        println(text)
    }

    fun error(text: String?) {
        System.err.println(text)
    }
}
