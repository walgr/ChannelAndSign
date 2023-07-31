package com.wpf.util.common.ui.channelset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.apiIcon
import com.wpf.util.common.ui.base.AbiType
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.marketplace.MarketPlaceViewModel
import com.wpf.util.common.ui.widget.common.ItemView
import com.wpf.util.common.ui.marketplace.markets.base.MarketApk
import com.wpf.util.common.ui.marketplace.markets.base.UploadState
import com.wpf.util.common.ui.marketplace.markets.base.UploadState.*
import com.wpf.util.common.ui.widget.common.ShapeText

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun ApkItem(marketApk: MarketApk) {

    val isSelect = remember { marketApk.isSelectState }
    val uploadState = remember { marketApk.uploadState }

    ItemView(modifier = Modifier.heightIn(min = 48.dp).combinedClickable(enabled = true, onClick = {
        isSelect.value = marketApk.click()
    })) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(isSelect.value, null)
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.offset(x = 8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = marketApk.marketType.name, fontSize = 14.sp)
                    Text(text = marketApk.versionName(), fontSize = 8.sp, modifier = Modifier.offset(x = 4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.offset(y = 4.dp)) {
                    if (marketApk.abiApk.find { it.abi == AbiType.Abi32 } != null) {
                        ShapeText("32", fontSize = 8.sp, bgColor = if (MarketPlaceViewModel.getCanApiMarketList().find {
                                it.name == marketApk.channelName
                            }?.uploadAbi()?.contains(AbiType.Abi32) == true) mainTextColor else Color.Gray)
                    }
                    if (marketApk.abiApk.find { it.abi == AbiType.Abi64 } != null) {
                        Box(modifier = Modifier.width(4.dp))
                        ShapeText("64", fontSize = 8.sp, bgColor = if (MarketPlaceViewModel.getCanApiMarketList().find {
                                it.name == marketApk.channelName
                            }?.uploadAbi()?.contains(AbiType.Abi64) == true) mainTextColor else Color.Gray)
                    }
                    if (marketApk.abiApk.find { it.abi == AbiType.Abi32_64 } != null) {
                        Box(modifier = Modifier.width(4.dp))
                        ShapeText(
                            "兼容包", fontSize = 8.sp, bgColor = if (MarketPlaceViewModel.getCanApiMarketList().find {
                                    it.name == marketApk.channelName
                                }?.uploadAbi()?.contains(AbiType.Abi32_64) == true) mainTextColor else Color.Gray
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd
            ) {
                Row {
                    Box(modifier = Modifier.size(if (uploadState.value != UPLOAD_WAIT) 24.dp else 0.dp).offset(4.dp)) {
                        when (uploadState.value) {
                            UPLOADING -> {
                                CircularProgressIndicator()
                            }
                            UPLOAD_SUCCESS -> {
                                Icon(Icons.Outlined.CheckCircle, "成功")
                            }
                            UPLOAD_FAIL -> {
                                Icon(Icons.Default.Close, "失败")
                            }
                            UPLOAD_WAIT -> {

                            }
                        }
                    }
                    if (marketApk.marketType.canApi()) {
                        apiIcon()
                    }
                }
            }
        }
    }
}