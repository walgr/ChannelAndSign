package com.wpf.utils.pgyupload.pgy.data

open class TokenResponse(
    val key: String,
    val endpoint: String,
    val params: Param? = null
)

data class Param(
    val signature: String,
    val `x-cos-security-token`: String,
    val key: String
)