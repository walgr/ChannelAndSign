package com.wpf.util.common.ui.http

import com.wpf.util.common.ui.utils.Callback
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

val client = HttpClient(CIO)

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

    fun get(serverUrl: String, params: Map<String, Any>? = null, callback: Callback<JsonObject>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            client.get(serverUrl)
        }
    }
}