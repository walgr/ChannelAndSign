package com.wpf.utils.pgyupload.pgy.http

interface SuccessCallback<T>: Callback<T> {

    override fun onFail(msg: String) {

    }
}

interface Callback<T> {

    fun onSuccess(t: T)

    fun onFail(msg: String)
}