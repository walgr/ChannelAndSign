package com.wpf.utils.pgyupload.pgy.data

data class CheckResponseInfo(
    val buildVersion: String,                       //版本号, 默认为1.0 (是应用向用户宣传时候用到的标识，例如：1.1、8.2.1等。)
    val forceUpdateVersion: String,                 //强制更新版本号（未设置强置更新默认为空）
    val forceUpdateVersionNo: String,               //强制更新的版本编号
    val needForceUpdate: Boolean,                   //是否强制更新
    val downloadURL: String,                        //应用安装地址
    val appURL: String,                             //应用安装单页地址
    val buildHaveNewVersion: Boolean,               //是否有新版本
    val buildVersionNo: String,                     //上传包的版本编号，默认为1 (即编译的版本号，一般来说，编译一次会变动一次这个版本号, 在 Android 上叫 Version Code。对于 iOS 来说，是字符串类型；对于 Android 来说是一个整数。例如：1001，28等。)
    val buildShortcutUrl: String,                   //应用二维码地址
    val buildCreated: String,                       //应用短链接
    val buildUpdateDescription: String,             //应用更新说明
)
