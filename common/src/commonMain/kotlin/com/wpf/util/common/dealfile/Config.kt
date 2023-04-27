package com.wpf.util.common.dealfile

import java.util.logging.Logger

//渠道基础文件路径
var channelBaseInsertFilePath = ""
    set(value) {
        field = value
    }
//存储渠道包位置
var channelSavePath = ""

//友盟渠道配置位置
var channelsFilePath = ""
    set(value) {
        field = value
    }


var zipalignFile = ""
//签名文件
var signFile = ""
//签名文件密码
var signPassword = ""
//签名文件别名
var signAlias = ""
//签名文件别名密码
var signAliasPassword = ""

//删除文件时延迟
var deleteFileDelay = 200L

//默认Logger
val defaultLog: Logger = Logger.getLogger("打渠道包")