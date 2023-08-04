package com.wpf.util.webview

import com.sun.javafx.scene.web.Debugger
import javafx.application.Platform
import javafx.util.Callback
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.io.IOException
import javax.servlet.ServletContext

object DevToolsDebuggerServer {
    private var contextHandler: ServletContextHandler? = null
    private var debugger: Debugger? = null
    private var server: Server? = null
    private var isStarting = false
    @Throws(Exception::class)
    fun startDebugServer(debugger: Debugger, debuggerPort: Int) {
        if (isStarting) return
        server = Server(debuggerPort)
        debugger.isEnabled = true
        debugger.sendMessage("{\"id\" : -1, \"method\" : \"Network.enable\"}")
        contextHandler = ServletContextHandler(ServletContextHandler.SESSIONS)
        contextHandler!!.setContextPath("/")
        val devToolsHolder = ServletHolder(DevToolsWebSocketServlet())
        contextHandler!!.addServlet(devToolsHolder, "/")
        server!!.setHandler(contextHandler)
        server!!.start()
        DevToolsDebuggerServer.debugger = debugger
        debugger.messageCallback = object : Callback<String?, Void?> {
            override fun call(data: String?): Void? {
                val mainSocket: DevToolsWebSocket? = contextHandler!!.servletContext
                    .getAttribute(DevToolsWebSocket.WEB_SOCKET_ATTR_NAME) as DevToolsWebSocket?
                if (mainSocket != null) {
                    try {
                        mainSocket.sendMessage(data)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                return null
            }
        }
        val remoteUrl = "chrome-devtools://devtools/bundled/inspector.html?ws=localhost:$debuggerPort/"
        println("To debug open chrome and load next url: $remoteUrl")
        isStarting = true
    }

    @Throws(Exception::class)
    fun stopDebugServer() {
        if (server != null) {
            server!!.stop()
            server!!.join()
        }
        isStarting = false
    }

    @JvmStatic
    fun sendMessageToBrowser(data: String?) {
        Platform.runLater(Runnable
        // Display.asyncExec won't be successful here
        { debugger!!.sendMessage(data) })
    }

    val serverState: String?
        get() = if (server == null) null else server!!.getState()
    @JvmStatic
    val servletContext: ServletContext?
        get() = if ((contextHandler != null)) contextHandler!!.servletContext else null
}