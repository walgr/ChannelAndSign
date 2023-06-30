package com.wpf.base.dealfile.util

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ThreadPoolHelper {
    private val executorsPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

    fun <T> run(
        runnable: List<Callable<T>>?,
        finish: ((List<T>?) -> Unit)? = null
    ) {
        if (runnable.isNullOrEmpty()) {
            finish?.invoke(null)
            return
        }
        val result: List<T> = executorsPool.invokeAll(runnable).map {
            it.get()
        }
        finish?.invoke(result)
    }
}