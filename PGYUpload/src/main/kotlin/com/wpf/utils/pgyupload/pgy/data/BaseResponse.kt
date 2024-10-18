package com.wpf.utils.pgyupload.pgy.data

open class BaseResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)