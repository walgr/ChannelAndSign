package com.wpf.base.dealfile.util

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

object ThreadPoolHelper {
    private val executorsPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun <T> run(
        runnable: List<Callable<T>>?,
        finish: ((List<Future<T>?>?) -> Unit)? = null
    ) {
        if (runnable.isNullOrEmpty()) {
            finish?.invoke(null)
            return
        }
        val result = executorsPool.invokeAll(runnable)
        finish?.invoke(result)
    }
}