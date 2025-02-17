package com.wpf.utils.pgyupload.pgy.data

data class ApkInfo(
    val buildKey: String,                           //Build Key是唯一标识应用的索引ID
    val buildType: Int,                             //应用类型（1:iOS; 2:Android）
    val buildIsFirst: Int,                          //是否是第一个App（1:是; 2:否）
    val buildIsLastest: Int,                        //是否是最新版（1:是; 2:否）
    val buildFileSize: Long,                         //App 文件大小
    val buildName: String,                          //应用名称
    val buildVersion: String,                       //版本号, 默认为1.0 (是应用向用户宣传时候用到的标识，例如：1.1、8.2.1等。)
    val buildVersionNo: String,                     //上传包的版本编号，默认为1 (即编译的版本号，一般来说，编译一次会变动一次这个版本号, 在 Android 上叫 Version Code。对于 iOS 来说，是字符串类型；对于 Android 来说是一个整数。例如：1001，28等。)
    val buildBuildVersion: String,                  //蒲公英生成的用于区分历史版本的build号
    val buildIdentifier: String,                    //应用程序包名，iOS为BundleId，Android为包名
    val buildIcon: String,                          //应用的Icon图标key，访问地址为 https://www.pgyer.com/image/view/app_icons/<buildIcon>
    val buildDescription: String,                   //应用介绍
    val buildUpdateDescription: String,             //应用更新说明
    val buildScreenShots: String,                   //应用截图的key，获取地址为 https://www.pgyer.com/image/view/app_screenshots/<screenshot_key>
    val buildShortcutUrl: String,                   //应用短链接
    val buildQRCodeURL: String,                     //应用二维码地址
    val buildCreated: String,                       //应用上传时间
    val buildUpdated: String,                       //应用更新时间
    var updateMsg: String,                          //应用更新文案
)
