package com.wpf.util.common.ui.http

import com.wpf.util.common.ui.utils.Callback
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

val client = HttpClient(CIO)

object Http {
    fun post(serverUrl: String, params: Map<String, Any>, header: Map<String, Any>, callback: Callback<JsonObject>) {
        CoroutineScope(Dispatchers.IO).launch {
            client.post(serverUrl)
        }
    }

    fun get(serverUrl: String, params: Map<String, Any>, callback: Callback<JsonObject>) {
        CoroutineScope(Dispatchers.IO).launch {
            client.get(serverUrl)
        }
    }
}