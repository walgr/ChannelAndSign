package com.wpf.utils.tools

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class LogStreamThread(
    private val inputStream: InputStream,
    private val showLogInLine: Boolean = true,
    private val showAllLog: ((log: String) -> Boolean)? = null
) : Thread() {
    override fun run() {
        super.run()
        val reader = InputStreamReader(inputStream)
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