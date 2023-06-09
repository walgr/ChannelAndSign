package com.wpf.util.common.ui.configset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wpf.base.dealfile.channelBaseInsertFilePath
import com.wpf.base.dealfile.channelSavePath
import com.wpf.base.dealfile.zipalignFile
import com.wpf.util.common.ui.utils.settings
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.FileSelector
import com.wpf.util.common.ui.widget.common.InputView

@Preview
@Composable
fun configPage() {
    //渠道基础文件位置
    var inputChannelBaseFilePath by remember { mutableStateOf(ConfigPageViewModel.getChannelBaseFilePath()) }
    //渠道保存位置
    var inputChannelSaveFilePath by remember { mutableStateOf(ConfigPageViewModel.getChannelSaveFilePath()) }
    //apk对齐工具位置
    var inputZipalignFilePath by remember { mutableStateOf(ConfigPageViewModel.getZipalignFilePath()) }

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
                        Text("软件配置", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                    ) {
                        Column(
                            modifier = Modifier.padding(all = 16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputView(input = inputChannelBaseFilePath, hint = "请输入渠道基础文件位置") {
                                    inputChannelBaseFilePath = it
                                    ConfigPageViewModel.saveChannelBaseFilePath(it)
                                    channelBaseInsertFilePath = ConfigPageViewModel.getChannelBaseFilePath()
                                }
                                Button(onClick = {
                                    FileSelector.showFileSelector(arrayOf("xml")) {
                                        inputChannelBaseFilePath = it
                                        ConfigPageViewModel.saveChannelBaseFilePath(it)
                                        channelBaseInsertFilePath = ConfigPageViewModel.getChannelBaseFilePath()
                                    }
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("选择")
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputView(input = inputChannelSaveFilePath, hint = "请输入渠道保存位置,默认当前目录") {
                                    inputChannelSaveFilePath = it
                                    ConfigPageViewModel.saveChannelSaveFilePath(it)
                                    channelSavePath = ConfigPageViewModel.getChannelSaveFilePath()
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputView(input = inputZipalignFilePath, hint = "请输入Apk对齐工具Zipalign位置") {
                                    inputZipalignFilePath = it
                                    ConfigPageViewModel.saveZipalignFilePath(it)
                                    zipalignFile = ConfigPageViewModel.getZipalignFilePath()
                                }
                                Button(onClick = {
                                    FileSelector.showFileSelector(arrayOf("exe")) {
                                        inputZipalignFilePath = it
                                        ConfigPageViewModel.saveZipalignFilePath(it)
                                        zipalignFile = ConfigPageViewModel.getZipalignFilePath()
                                    }
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("选择")
                                }
                            }
                            Button(onClick = {
                                settings.clear()
                            }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("清空所有数据")
                            }
                        }
                    }
                }
            }
        }

    }
}