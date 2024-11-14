package com.wpf.utils.tools

import com.wpf.utils.isMacRuntime
import com.wpf.utils.isWinRuntime

object ProgressUtil {

    fun stop(pid: String) {
        runCatching {
            val killP = if (isMacRuntime) {
                Runtime.getRuntime().exec("kill -9 $pid")
            } else if (isWinRuntime) {
                Runtime.getRuntime().exec("cmd /c taskkill /PID $pid /F /T")
            } else {
                Runtime.getRuntime().exec("kill $pid")
            }
            LogStreamThread(killP.inputStream, true).start()
            LogStreamThread(killP.errorStream, true).start()
        }.getOrElse {
            println("无法停止进程（$pid）：" + it.message)
        }
    }
}