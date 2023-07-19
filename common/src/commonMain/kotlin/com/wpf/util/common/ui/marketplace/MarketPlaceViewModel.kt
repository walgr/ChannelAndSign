package com.wpf.util.common.ui.marketplace

import com.wpf.util.common.ui.channelset.Channel
import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.marketplace.markets.HuaweiMarket
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketType
import com.wpf.util.common.ui.marketplace.markets.XiaomiMarket
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.settings

object MarketPlaceViewModel {

    fun getSelectMarket(channelList: List<Channel>? = null, marketList: List<Market>? = null): Market {
        return getSelectMarket(
            (channelList ?: ChannelSetViewModel.getChannelList()).find { it.isSelect }?.name ?: "",
            (marketList ?: getCanApiMarketList()).find { it.isSelect }!!.name
        )
    }

    private val marketMap = mutableMapOf<String, Market>()
    fun getSelectMarket(channelName: String, marketName: String): Market {
        val key = "Channel${channelName}Market${marketName}"
        val market = marketMap[key]
        if (market is XiaomiMarket) {
            market.initPubkey()
        }
        if (market != null) {
            return market
        }
        val dataJson = settings.getString(key, "{}")
        val saveMarket = gson.fromJson(dataJson, getCanApiMarketList().find { it.name == marketName }!!.javaClass)
        saveMarket?.changeSelect(getCanApiMarketList().find { it.name == marketName }?.isSelect ?: false)
        if (saveMarket is XiaomiMarket) {
            saveMarket.initPubkey()
        }
        marketMap[key] = saveMarket
        return saveMarket
    }

    fun getCanApiMarketList(): List<Market> {
        val marketList = MarketType.values().filter { marketType ->
            marketType.canApi()
        }.map { marketType ->
            marketType.market
        }.toMutableList()
        if (marketList.find { find -> find.isSelect } == null) {
            marketList[0].changeSelect(true)
        }
        marketList.forEach { market ->
            if (market is XiaomiMarket) {
                market.initPubkey()
            }
        }
        return marketList
    }

    fun saveMarketList(channelList: List<Channel>, marketSelect: Market) {
        val channelSelect = channelList.find { it.isSelect } ?: return
        val key = "Channel${channelSelect.name}Market${marketSelect.name}"
        marketMap.remove(key)
        settings.putString(key, gson.toJson(marketSelect))
        if (marketSelect is HuaweiMarket) {
            //换信息后清空token
            marketSelect.clearToken()
        }
    }
}