package com.wpf.util.common.ui.marketplace

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.marketplace.markets.base.Market
import com.wpf.util.common.ui.marketplace.markets.base.MarketTypeHelper
import com.wpf.util.common.ui.widget.AddItemDialog
import com.wpf.util.common.ui.widget.common.BigTitle
import com.wpf.util.common.ui.widget.common.ItemTextView
import com.wpf.util.common.ui.widget.common.Title

@Preview
@Composable
fun marketPlacePage() {

    val showAddMarketDialog = remember { mutableStateOf(false) }
    val marketList = remember { mutableStateListOf(*MarketPlaceViewModel.getCanApiMarketList().toTypedArray()) }
    //分组列表
    val channelList = remember { mutableStateListOf(*ChannelSetViewModel.getChannelList().toTypedArray()) }

    val selectMarket = remember { mutableStateOf(MarketPlaceViewModel.getSelectMarket()) }

    Box {
        Row {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(shape = RoundedCornerShape(8.dp))
                    .background(color = itemBgColor)
            ) {
                Column {
                    BigTitle("市场配置")
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp, 0.dp, 16.dp, 16.dp)
                    ) {
                        Row {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(0.dp, 0.dp, 4.dp, 0.dp)
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                            ) {
                                Column {
                                    Title("市场")
                                    Row {
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(channelList) {
                                                ItemTextView(it.name, modifier = Modifier.padding(end = 0.dp).clickable {
                                                    channelList.forEach { market ->
                                                        market.changeSelect(false)
                                                    }
                                                    it.changeSelect(true)
                                                    selectMarket.value = MarketPlaceViewModel.getSelectMarket(channelList, marketList)
                                                }, isSelectState = it.isSelectState)
                                            }
                                        }
                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(marketList) {
                                                ItemTextView(it.name, modifier = Modifier.padding(start = 0.dp).clickable {
                                                    marketList.forEach { market ->
                                                        market.changeSelect(false)
                                                    }
                                                    it.changeSelect(true)
                                                    selectMarket.value = MarketPlaceViewModel.getSelectMarket(channelList, marketList)
                                                }, isSelectState = it.isSelectState)
                                            }
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.weight(2f).fillMaxHeight().padding(4.dp, 0.dp, 0.dp, 0.dp)
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                            ) {
                                Column(
                                    modifier = Modifier.padding(
                                        start = 8.dp,
                                        top = 4.dp,
                                        end = 8.dp,
                                        bottom = 4.dp
                                    )
                                ) {
                                    Title("市场配置")
                                    selectMarket.value.dispositionView(selectMarket.value)
                                    Button(onClick = {
                                        MarketPlaceViewModel.saveMarketList(channelList, selectMarket.value)
                                    }, modifier = Modifier.padding(start = 8.dp)) {
                                        Text("保存")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AddItemDialog(showAddMarketDialog) {
            showAddMarketDialog.value = false
            MarketTypeHelper.find(it)?.let { market ->
                marketList.add(market.market)
            }
        }
    }
}