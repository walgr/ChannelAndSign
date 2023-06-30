package com.wpf.util.common.ui.marketplace

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.channelset.*
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.DropBoxPanel
import com.wpf.util.common.ui.utils.FileSelector
import javax.swing.JFileChooser

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun marketPlacePage(window: ComposeWindow) {

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
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(0.dp, 0.dp, 5.dp, 0.dp)
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "市场",
                                            fontWeight = FontWeight.Bold,
                                            color = mainTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd
                                        ) {

                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.weight(2f).fillMaxHeight().padding(5.dp, 0.dp, 0.dp, 0.dp)
                            ) {

                            }
                        }
                    }
                }
            }
        }
    }
}