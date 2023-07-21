package com.wpf.util.common.ui.utils


object OnApplicationExit {

    fun exit(callback: (() -> Unit)? = null) {
        callback?.invoke()
    }
}