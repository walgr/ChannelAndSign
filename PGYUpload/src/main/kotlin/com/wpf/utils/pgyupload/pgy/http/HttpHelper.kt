package com.wpf.utils.pgyupload.pgy.http

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

object HttpHelper {

    val client: HttpClient by lazy {
        HttpClient(CIO) {
            engine {
                requestTimeout = 120000
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120000
                connectTimeoutMillis = 120000
            }
        }
    }

    fun apkHeader(fileName: String): Headers {
        return Headers.build {
            append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
            append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
        }
    }

    inline fun <reified T : Any> get(
        serverUrl: String,
        crossinline request: HttpRequestBuilder.() -> Unit = {},
        noinline callback: ((T?) -> Unit)? = null
    ) {
        runBlocking {
            runCatching {
                client.get(serverUrl, request).callbackResponse<T>(callback)
            }.onFailure {
                println(it.message)
                callback?.invoke(null)
            }
        }
    }

    inline fun <reified T : Any> post(
        serverUrl: String,
        crossinline request: HttpRequestBuilder.() -> Unit = {},
        noinline callback: ((T?) -> Unit)? = null
    ) {
        runBlocking {
            runCatching {
                client.post(serverUrl, request).callbackResponse<T>(callback)
            }.onFailure {
                println(it.message)
                callback?.invoke(null)
            }
        }
    }

    suspend inline fun <reified T : Any> HttpResponse.callbackResponse(noinline callback: ((T?) -> Unit)? = null) {
        if (status == HttpStatusCode.OK || status == HttpStatusCode.NoContent) {
            if (T::class == String::class) {
                callback?.invoke(bodyAsText() as T)
            } else {
                callback?.invoke(Gson().fromJson(bodyAsText(), object : TypeToken<T>() {}.type))
            }
        } else {
            callback?.invoke(null)
        }
    }
}