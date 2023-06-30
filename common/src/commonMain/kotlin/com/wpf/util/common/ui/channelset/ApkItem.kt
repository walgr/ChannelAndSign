package com.wpf.util.common.ui.channelset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.apiIcon
import com.wpf.util.common.ui.base.ItemView
import com.wpf.util.common.ui.marketplace.MarketApk

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun ApkItem(marketApk: MarketApk) {

    val isSelect = remember { marketApk.isSelectState }

    ItemView(
        modifier = Modifier.combinedClickable(
            enabled = true,
            onClick = {
                isSelect.value = marketApk.click()
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(isSelect.value, null)
            Text(text = marketApk.marketType.channelName, fontSize = 14.sp)
            if (marketApk.marketType.canApi) {
                Box(
                    modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier.width(44.dp).height(44.dp), contentAlignment = Alignment.CenterEnd
                    ) {
                        apiIcon()
                    }
                }
            }
        }
    }
}