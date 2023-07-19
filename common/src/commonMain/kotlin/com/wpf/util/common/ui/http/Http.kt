package com.wpf.util.common.ui.http

import com.wpf.util.common.ui.utils.Callback
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val client = HttpClient(CIO) {
    engine {
//        proxy = ProxyBuilder.http(
//            "http://127.0.0.1:8888"
//        )
        requestTimeout = 30000
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
        connectTimeoutMillis = 30000
    }
    install(ContentNegotiation) {
        json()
    }
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL
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

    fun submitForm(serverUrl: String, formParameters: Parameters, block: HttpRequestBuilder.() -> Unit = {}, callback: Callback<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseData = client.submitForm(serverUrl, formParameters, block = block)
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

    fun put(serverUrl: String, request: HttpRequestBuilder.() -> Unit = {}, callback: Callback<String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val responseData = client.put(serverUrl, request)
            if (responseData.status == HttpStatusCode.OK) {
                callback?.onSuccess(responseData.bodyAsText())
            } else {
                callback?.onFail(responseData.bodyAsText())
            }
        }
    }
}