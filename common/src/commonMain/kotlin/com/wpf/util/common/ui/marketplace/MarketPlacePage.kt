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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.marketplace.markets.base.MarketTypeHelper
import com.wpf.util.common.ui.widget.AddItemDialog
import com.wpf.util.common.ui.widget.common.InputView
import com.wpf.util.common.ui.widget.common.ItemTextView
import com.wpf.util.common.ui.widget.common.Title

@Preview
@Composable
fun marketPlacePage(window: ComposeWindow) {

    val showAddMarketDialog = remember { mutableStateOf(false) }
    val marketList = remember { mutableStateListOf(*MarketPlaceViewModel.getMarketSaveList().toTypedArray()) }

    Box {
        Row {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(shape = RoundedCornerShape(8.dp))
                    .background(color = Color(1f, 1f, 1f, 0.6f))
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(all = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("市场配置", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
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
                                    LazyColumn {
                                        items(marketList) {
                                            ItemTextView(it.name, modifier = Modifier.clickable {
                                                marketList.forEach { market ->
                                                    market.changeSelect(false)
                                                }
                                                it.changeSelect(true)
                                                MarketPlaceViewModel.saveMarketList(marketList)
                                            }, isSelectState = it.isSelectState)
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
                                    marketList.forEach {
                                        it.dispositionView(it)
                                    }
                                    Button(onClick = {
                                        MarketPlaceViewModel.saveMarketList(marketList)
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