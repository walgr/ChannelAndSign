package com.wpf.util.common.ui.marketplace

import com.google.gson.reflect.TypeToken
import com.wpf.util.common.ui.marketplace.markets.Market
import com.wpf.util.common.ui.marketplace.markets.MarketType
import com.wpf.util.common.ui.marketplace.markets.XiaomiMarket
import com.wpf.util.common.ui.utils.gson
import com.wpf.util.common.ui.utils.settings

object MarketPlaceViewModel {

    fun getMarketSaveList(): List<Market> {
        runCatching {
            gson.fromJson<List<Pair<String, String>>>(
                settings.getString("marketList", "[]"),
                object : TypeToken<List<Pair<String, String>>>() {}.type
            ).let {
                val marketList = MarketType.values().filter { marketType ->
                    marketType.canApi
                }.map { marketType ->
                    marketType.market
                }.toMutableList()
                it.forEach { pair ->
                    marketList.find { curList ->
                        curList.name == pair.first.split("_")[1]
                    }?.let { market ->
                        marketList.remove(market)
                        runCatching {
                            val newMarket = gson.fromJson(pair.second, market::class.java)
                            newMarket.isSelectState.value = newMarket.isSelect
                            marketList.add(newMarket)
                        }
                    }
                }
                if (marketList.find { find -> find.isSelect } == null) {
                    marketList[0].isSelectState.value = true
                }
                marketList.forEach { market ->
                    if (market is XiaomiMarket) {
                        market.initPubkey()
                    }
                }
                return marketList
            }
        }
        return emptyList()
    }

    fun saveMarketList(marketList: List<Market>) {
        settings.putString("marketList", gson.toJson(marketList.map {
            Pair("Market_${it.name}", gson.toJson(it))
        }))
    }
}