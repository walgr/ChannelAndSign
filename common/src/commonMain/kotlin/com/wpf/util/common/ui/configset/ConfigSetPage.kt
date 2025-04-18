package com.wpf.util.common.ui.configset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.reflect.TypeToken
import com.russhwolf.settings.get
import com.wpf.base.dealfile.channelBaseInsertFilePath
import com.wpf.base.dealfile.channelSavePath
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.common.InputView
import java.io.File

@Preview
@Composable
fun configPage() {
    //渠道基础文件位置
    val inputChannelBaseFilePath by autoSaveComposable("channelBaseFilePath") { remember { mutableStateOf("") } }
    //渠道保存位置
    val inputChannelSaveFilePath by autoSaveComposable("channelSaveFilePath") { remember { mutableStateOf("") } }

    Box {
        Row {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(shape = RoundedCornerShape(8.dp))
                    .background(color = itemBgColor)
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
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp).clip(shape = RoundedCornerShape(8.dp))
                            .background(color = centerBgColor),
                    ) {
                        Column(
                            modifier = Modifier.padding(all = 16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputView(
                                    modifier = Modifier.weight(1f),
                                    input = inputChannelBaseFilePath,
                                    hint = "请输入渠道基础文件位置"
                                ) {
                                    inputChannelBaseFilePath.value = it
                                    channelBaseInsertFilePath = inputChannelBaseFilePath.value
                                }
                                Button(onClick = {
                                    FileSelector.showFileSelector(arrayOf("xml")) {
                                        inputChannelBaseFilePath.value = it
                                        channelBaseInsertFilePath = inputChannelBaseFilePath.value
                                    }
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("选择")
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically
                            ) {
                                InputView(
                                    modifier = Modifier.weight(1f),
                                    input = inputChannelSaveFilePath, hint = "请输入渠道保存位置,默认当前目录"
                                ) {
                                    inputChannelSaveFilePath.value = it
                                    channelSavePath = inputChannelSaveFilePath.value
                                }
                                Button(onClick = {
                                    FileSelector.showFileSelector(arrayOf("xml")) {
                                        inputChannelSaveFilePath.value = it
                                        channelSavePath = inputChannelSaveFilePath.value
                                    }
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("选择")
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Button(onClick = {
                                    val allDataMap = mapOf(*settings.keys.map {
                                        Pair(it, gson.toJson(settings.get<String>(it)))
                                    }.toTypedArray())
                                    val allDataJson = mapGson.toJson(allDataMap)
                                    FileSelector.saveFile(extension = "json", info = allDataJson)
                                }) {
                                    Text("备份数据")
                                }
                                Button(onClick = {
                                    FileSelector.showFileSelector(arrayOf("json")) {
                                        val allDataJson = File(it).readText()
                                        val allDataMap = mapGson.fromJson<Map<String, String>>(
                                            allDataJson, object : TypeToken<Map<String, String>>() {}.type
                                        )
                                        if (allDataMap.isNotEmpty()) {
                                            settings.clear()
                                            allDataMap.forEach { (t, u) ->
                                                var realValue = if (u.startsWith("\"") && u.endsWith("\"")) {
                                                    u.substring(1, u.length - 1)
                                                } else u
                                                realValue = realValue.replace("\\\"", "\"")
                                                realValue = realValue.replace("\\\\", "\\")
                                                settings.putString(t, realValue)
                                            }
                                        }
                                    }
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("恢复数据")
                                }
                                Button(onClick = {
                                    settings.clear()
                                }, modifier = Modifier.padding(start = 8.dp)) {
                                    Text("清空所有数据")
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
