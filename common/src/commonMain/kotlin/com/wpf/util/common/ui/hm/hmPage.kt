package com.wpf.util.common.ui.hm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.channelset.ApkItem
import com.wpf.util.common.ui.channelset.ChannelSetViewModel
import com.wpf.util.common.ui.channelset.Path
import com.wpf.util.common.ui.channelset.dealClientClick
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.marketplace.markets.base.upload
import com.wpf.util.common.ui.uploadIcon
import com.wpf.util.common.ui.utils.checkWinPath
import com.wpf.util.common.ui.utils.onExternalDrag
import com.wpf.util.common.ui.widget.common.AddImage
import com.wpf.util.common.ui.widget.common.InputView
import com.wpf.util.common.ui.widget.common.ItemView
import com.wpf.util.common.ui.widget.common.SelectFileAddTitle
import com.wpf.util.common.ui.widget.common.Title
import com.wpf.utils.tools.HDCUtil
import kotlin.text.ifEmpty

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun hmPage() {
    Box(modifier = Modifier.fillMaxSize().background(color = itemBgColor)) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp).padding(all = 16.dp)
                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                contentAlignment = Alignment.Center
            ) {
                Text("鸿蒙远程更新", fontWeight = FontWeight.Bold, color = mainTextColor)
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp, 0.dp, 16.dp, 16.dp)
            ) {
                Row {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(0.dp, 0.dp, 4.dp, 0.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                    ) {
                        Title("客户端", rightIcon = Icons.Default.Refresh, rightContentDescription = "刷新") {
                            HDCUtil.getClientList()
                        }
                    }

                }
            }
        }
    }
}