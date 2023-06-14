package com.wpf.base.dealfile.util

import java.util.concurrent.Callable
import java.util.concurrent.Executors

object ThreadPoolHelper {

    fun <T> create(runnable: Callable<T>, finish: () -> Unit) {
        val executorsPool = Executors.newWorkStealingPool()
        executorsPool.submit(runnable)
    }
}