package com.wpf.util.common.ui.marketplace.markets.base

import java.io.File

data class XiaomiPushData(
    val userName: String,                   //用户名，在小米开发者站登录的邮箱
    val synchroType: Int = 1,               //更新类型：0=新增，1=更新包，2=内容更新
    val appInfo: XiaomiApk,                    //应用包实体JSON 字符串
    @Transient val apk: File,                          //应用包实体
    @Transient val secondApk: File?,                   //应用包实体
    @Transient val icon: File? = null,                 //应用包Icon
    @Transient val screenShot1: File?= null,          //应用的第1 幅截图，synchroType=0 时必选
    @Transient val screenShot2: File?= null,          //应用的第2 幅截图，synchroType=0 时必选
    @Transient val screenShot3: File?= null,          //应用的第3 幅截图，synchroType=0 时必选
    @Transient val screenShot4: File?= null,          //应用的第4 幅截图，synchroType=0 时必选
    @Transient val screenShot5: File?= null,          //应用的第5 幅截图，synchroType=0 时必选
)


data class XiaomiApk(
    val appName: String,
    val packageName: String,
    val publisherName: String? = null,
    val versionName: String? = null,
    val category: String? = null,
    val keyWords: String? = null,
    val desc: String? = null,
    val updateDesc: String,
    val shortDesc: String? = null,
    val web: String? = null,
    val price: String? = null,
    val privacyUrl: String? = null,
)