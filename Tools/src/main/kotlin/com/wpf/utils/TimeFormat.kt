package com.wpf.utils

import java.text.SimpleDateFormat
import java.util.Date

fun Long.formatTime(): String {
    val minute = (this / 1000) / 60
    val second = (this / 1000) % 60
    val millis = this % 1000
    return if (minute > 0) {
        "%02d分%02d秒%02d毫秒".format(minute, second, millis)
    } else if (second > 0) {
        "%02d秒%02d毫秒".format(second, millis)
    } else "%02d毫秒".format(millis)
}

fun Long.formatToSimpleData(): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(this))
}