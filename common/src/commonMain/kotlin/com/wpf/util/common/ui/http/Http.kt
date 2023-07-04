package com.wpf.util.common.ui.http

import com.wpf.util.common.ui.utils.Callback
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val client = HttpClient(CIO) {
    engine {
        proxy = ProxyBuilder.http(
            "http://127.0.0.1:8888"
        )
    }
}

object Http {
    fun post(serverUrl: String, request: HttpRequestBuilder.() -> Unit = {}, callback: Callback<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseData = client.post(serverUrl, request)
            if (responseData.status == HttpStatusCode.OK) {
                callback?.onSuccess(responseData.bodyAsText())
            } else {
                callback?.onFail(responseData.bodyAsText())
            }

        }
    }

    fun submitForm(serverUrl: String, formParameters: Parameters, callback: Callback<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseData = client.submitForm(serverUrl, formParameters)
            if (responseData.status == HttpStatusCode.OK) {
                callback?.onSuccess(responseData.bodyAsText())
            } else {
                callback?.onFail(responseData.bodyAsText())
            }
        }
    }

    fun get(serverUrl: String, request: HttpRequestBuilder.() -> Unit = {}, callback: Callback<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseData = client.get(serverUrl, request)
            if (responseData.status == HttpStatusCode.OK) {
                callback?.onSuccess(responseData.bodyAsText())
            } else {
                callback?.onFail(responseData.bodyAsText())
            }
        }
    }
}