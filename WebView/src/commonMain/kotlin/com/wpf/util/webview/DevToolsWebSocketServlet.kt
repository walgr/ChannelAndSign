package com.wpf.util.webview

import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory

class DevToolsWebSocketServlet : WebSocketServlet() {
    override fun configure(factory: WebSocketServletFactory) {
        factory.register(DevToolsWebSocket::class.java)
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}