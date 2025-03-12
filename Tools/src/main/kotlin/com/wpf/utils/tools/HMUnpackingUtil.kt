package com.wpf.utils.tools

import ohos.UncompressEntrance
import java.io.File

class HMHapFileInfo {
    var appName: String? = null
    var bundleName: String? = null
    var versionName: String? = null
    var versionCode: String? = null
    var releaseType: String? = null
    var abilityName: String? = null
}

object HMUnpackingUtil {

    fun getHapInfo(hapFile: File): HMHapFileInfo? {

        val result = UncompressEntrance.parseHap(hapFile.path)
        if (result.result) {
            val info = HMHapFileInfo()
            info.appName = result.profileInfos.getOrNull(0)?.appInfo?.appName
            info.bundleName = result.profileInfos.getOrNull(0)?.appInfo?.bundleName
            info.versionName = result.profileInfos.getOrNull(0)?.appInfo?.versionName
            info.versionCode = result.profileInfos.getOrNull(0)?.appInfo?.versionCode
            info.releaseType = result.profileInfos.getOrNull(0)?.appInfo?.releaseType
            info.abilityName = result.profileInfos.getOrNull(0)?.hapInfo?.abilities?.getOrNull(0)?.name
            return info
        } else {
            println("获取信息失败:" + result.message)
        }
        return null
    }
}