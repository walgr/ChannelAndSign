package com.wpf.utils.tools

import com.wpf.utils.isWinRuntime
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

class LogStreamThread(
    private val inputStream: InputStream,
    private val showLogInLine: Boolean = true,
    private val showAllLog: ((log: String) -> Boolean)? = null
) : Thread() {
    override fun run() {
        super.run()
        val reader = InputStreamReader(inputStream, Charset.forName(if (isWinRuntime) "GBK" else "utf-8"))
        val bf = BufferedReader(reader)
        var line: String?
        val showAll = showAllLog != null
        val allLogBuilder = StringBuilder()
        kotlin.runCatching {
            do {
                line = bf.readLine()
                if (showAll) {
                    allLogBuilder.append(line)
                }
                if (line != null) {
                    if (showLogInLine) {
                        println(line)
                    }
                }
            } while (line != null)
            inputStream.close()
            if (showAll) {
                val allLog = allLogBuilder.toString()
                if (showAllLog?.invoke(allLog) == true) {
                    println(allLog)
                }
            }
        }
    }
}