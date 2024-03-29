package com.wpf.jiagu

import android.app.Application
import android.util.Log

class JiaguApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val jiaguConfig = assets.open("jiagu.config")
        Log.e("JiaguApp", jiaguConfig.readBytes().decodeToString())
        val jiaguDexList = assets.list("")?.filter {
            it.endsWith(".wpfjiagu")
        }?.map {
            assets.open(it)
        } ?: return
        Log.e("JiaguApp", jiaguDexList.joinToString())
    }
}