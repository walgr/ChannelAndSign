package com.wpf.util.common.ui.utils

interface Callback<T> {

    fun onSuccess(t: T)

    fun onFail(msg: String)
}