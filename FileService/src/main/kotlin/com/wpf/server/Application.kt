package com.wpf.server

import com.wpf.server.plugins.configureRouting
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*

fun main() {
    startServer()
}

fun startServer(): ApplicationEngine {
    return embeddedServer(CIO, port = 6457, host = FileServer.HOST, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()
}

object FileServer {
    var serverBasePath = ""
    const val HOST = "127.0.0.1"
    const val BASE_URL = "http://$HOST:6457"

    private var engine: ApplicationEngine? = null
    fun start() {
        engine = startServer()
//        println("服务已开启")
    }

    fun stop() {
        println("服务已关闭")
        engine?.stop()
    }
}
