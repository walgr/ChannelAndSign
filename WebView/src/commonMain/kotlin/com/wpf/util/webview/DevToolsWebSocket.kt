//package com.wpf.util.webview
//
//import com.wpf.util.webview.DevToolsDebuggerServer.sendMessageToBrowser
//import com.wpf.util.webview.DevToolsDebuggerServer.servletContext
//import org.eclipse.jetty.websocket.api.Session
//import org.eclipse.jetty.websocket.api.WebSocketListener
//import java.io.IOException
//import java.text.MessageFormat
//import javax.servlet.ServletContext
//
//class DevToolsWebSocket : WebSocketListener {
//    private var session: Session? = null
//    private val context: ServletContext?
//
//    init {
//        context = servletContext
//    }
//
//    override fun onWebSocketConnect(session: Session) {
//        this.session = session
//        if (context!!.getAttribute(WEB_SOCKET_ATTR_NAME) != null) {
//            session.close()
//            println("Another client is already connected. Connection refused")
//        } else {
//            context.setAttribute(WEB_SOCKET_ATTR_NAME, this)
//            println("Client connected")
//        }
//    }
//
//    override fun onWebSocketClose(closeCode: Int, message: String) {
//        val mainSocket = context!!.getAttribute(WEB_SOCKET_ATTR_NAME) as DevToolsWebSocket
//        if (mainSocket === this) {
//            context.removeAttribute(WEB_SOCKET_ATTR_NAME)
//            println("Client disconnected")
//        }
//    }
//
//    @Throws(IOException::class)
//    fun sendMessage(data: String?) {
//        val remote = session!!.remote
//        remote.sendString(data)
//    }
//
//    override fun onWebSocketText(data: String) {
//        sendMessageToBrowser(data)
//    }
//
//    override fun onWebSocketError(t: Throwable) {
//        val errorMessage = t.message
//        println(MessageFormat.format("WebSocket error occurred: {0}", errorMessage))
//    }
//
//    override fun onWebSocketBinary(arg0: ByteArray, arg1: Int, arg2: Int) {}
//
//    companion object {
//        const val WEB_SOCKET_ATTR_NAME = "org.javafx.devtools.DevToolsWebSocket"
//    }
//}