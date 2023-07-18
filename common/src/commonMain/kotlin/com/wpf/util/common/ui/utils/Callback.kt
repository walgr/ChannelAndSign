package com.wpf.util.common.ui.utils

interface SuccessCallback<T>: Callback<T> {

    override fun onFail(msg: String) {

    }
}

interface Callback<T> {

    fun onSuccess(t: T)

    fun onFail(msg: String)
}