package com.wpf.base.dealfile.util

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

object ThreadPoolHelper {

    fun <T> run(
        nThreads: Int = Runtime.getRuntime().availableProcessors(),
        runnable: List<Callable<T>>?,
        finish: (List<Future<T>>?) -> Unit
    ) {
        if (runnable == null) {
            finish.invoke(null)
            return
        }
        val executorsPool = Executors.newFixedThreadPool(nThreads)
        finish.invoke(executorsPool.invokeAll(runnable))
    }
}