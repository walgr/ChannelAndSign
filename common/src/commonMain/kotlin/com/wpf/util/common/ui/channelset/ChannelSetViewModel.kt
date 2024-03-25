package com.wpf.util.common.ui.channelset

import com.google.gson.reflect.TypeToken
import com.wpf.base.dealfile.*
import com.wpf.util.common.ui.utils.settings
import com.wpf.util.common.ui.base.Apk
import com.wpf.util.common.ui.configset.ConfigPageViewModel
import com.wpf.util.common.ui.marketplace.markets.base.MarketApk
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.signset.SignFile
import com.wpf.util.common.ui.utils.channelName
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.marketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object ChannelSetViewModel {

    fun getClientList(): List<Client> {
        settings.getStringOrNull("clientList")?.let {
            return gson.fromJson(it, object : TypeToken<List<Client>>() {}.type)
        }
        return arrayListOf()
    }

    fun getChannelDataInFile(txtFilePath: String): List<Array<String>> {
        val channelsFile = File(txtFilePath)
        val result = mutableListOf<Array<String>>()
        if (!channelsFile.exists()) return result
        runCatching {
            channelsFile.forEachLine {
                if (it.isNotEmpty()) {
                    val fields = it.split(" ")
                    val channelApkFileName: String = fields[1].trim().replace("\n", "")
                    val channelName: String = fields[2].trim().replace("\n", "")
                    result.add(arrayOf(channelApkFileName, channelName))
                }
            }
        }
        return result
    }

    fun dealApk(filePathList: List<String>, channelPath: String?, sign: SignFile, callback: (() -> Unit)) {
        channelsFilePath = channelPath ?: ""
        channelBaseInsertFilePath = ConfigPageViewModel.getChannelBaseFilePath()
        channelSavePath = ConfigPageViewModel.getChannelSaveFilePath()
        signFile = sign.StoreFile
        signPassword = sign.StorePass
        signAlias = sign.KeyAlias
        signAliasPassword = sign.KeyPass
        var returnTime = 0
        CoroutineScope(Dispatchers.Default).launch {
            filePathList.forEach {
                ChannelAndSign.scanFile(it, exitProcess = false) {
                    returnTime++
                    if (returnTime == filePathList.size) {
                        //运行结束
                        callback.invoke()
                    }
                }
            }
        }
    }

    fun dealMargetPlace(pathList: List<String>): MutableList<MarketApk> {
        var marketPlaceApkList: MutableList<MarketApk> = mutableListOf()
        pathList.map {
            channelSavePath.ifEmpty { if (it.contains(".apk")) File(it).parent else it }
        }.forEach { path ->
            File(path).listFiles()?.filter {
                it.nameWithoutExtension.contains("sign")
                        && MarketType.entries.find { market ->
                    it.nameWithoutExtension.contains(market.channelName, ignoreCase = true)
                } != null
            }?.let {
                it.forEach { file ->
                    val findMarket = marketPlaceApkList.find { marketPlaceApkList ->
                        marketPlaceApkList.channelName == file.nameWithoutExtension.channelName()
                    }
                    val apk = Apk(
                        name = file.nameWithoutExtension,
                        size = file.length(),
                        filePath = file.path,
                        fileName = file.name
                    )
                    findMarket?.abiApk?.add(apk)
                    if (findMarket == null) {
                        marketPlaceApkList.add(
                            MarketApk(
                                apk.channelName.marketType(),
                                file.nameWithoutExtension.channelName() ?: "",
                                arrayListOf(apk)
                            )
                        )
                    }
                }
                marketPlaceApkList = marketPlaceApkList.sortedBy { market ->
                    !market.isSelect
                }.toMutableList()
            }
        }
        return marketPlaceApkList
    }
}