package com.wpf.util.common.ui.hm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.FileSelector
import com.wpf.util.common.ui.utils.autoSaveComposable
import com.wpf.util.common.ui.widget.common.InputView
import com.wpf.util.common.ui.widget.common.Title
import com.wpf.utils.formatToSimpleData
import com.wpf.utils.tools.HDCUtil
import com.wpf.utils.tools.HMClientInfo
import com.wpf.utils.tools.HMUnpackingUtil
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun hmPage() {
    val hmVM = HmVM
    var hmClientList = remember { mutableStateListOf<HMClientInfo>() }
    hmClientList.clear()
    hmClientList.addAll(hmVM.getClientList())
    var serverHmHapFile = remember { mutableStateListOf<ServerHapInfo>() }
    val hmClientConnectAddress = autoSaveComposable("hmClientConnectAddress") { remember { mutableStateOf("") } }
    val hmServerConnectAddress = autoSaveComposable("hmServerConnectAddress") { remember { mutableStateOf("") } }
    val hmServerPackageName = autoSaveComposable("hmServerPackageName") { remember { mutableStateOf("") } }
    val hmServerVersionName = autoSaveComposable("hmServerVersionName") { remember { mutableStateOf("") } }
    hmVM.getDownloadFile()?.let {
        serverHmHapFile.clear()
        serverHmHapFile.add(it)
    }
    hmVM.setDownloadData(hmServerConnectAddress.value, hmServerPackageName.value, hmServerVersionName.value) {
        serverHmHapFile.clear()
        serverHmHapFile.add(it)
    }
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
                    Box(modifier = Modifier.weight(3f).fillMaxHeight()) {
                        Column(
                            modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                                    .padding(8.dp)
                            ) {
                                Title("设备连接")

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InputView(
                                        modifier = Modifier.weight(1f).padding(end = 16.dp),
                                        input = hmClientConnectAddress,
                                        hint = "连接地址"
                                    ) {
                                        hmClientConnectAddress.value = it
                                    }
                                    Button({
                                        if (HDCUtil.connectClient(hmClientConnectAddress.value)) {
                                            hmClientList.clear()
                                            hmClientList.addAll(hmVM.getClientList())
                                        }
                                    }) {
                                        Text("连接")
                                    }
                                }

                            }
                            Column(
                                modifier = Modifier.weight(2f).fillMaxHeight().padding(0.dp, 8.dp, 0.dp, 0.dp)
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                            ) {
                                Title(
                                    "当前连接客户端",
                                    rightIcon = Icons.Default.Refresh,
                                    rightContentDescription = "刷新"
                                ) {
                                    hmClientList.clear()
                                    hmClientList.addAll(hmVM.getClientList())
                                }
                                LazyColumn {
                                    items(hmClientList) { client ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                                .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                                .clip(shape = RoundedCornerShape(8.dp))
                                                .background(color = Color.White),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                client.name,
                                                color = Color.Black,
                                                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp).fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(3f).fillMaxHeight()) {
                        Column(
                            modifier = Modifier.padding(0.dp, 0.dp, 8.dp, 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(2f).fillMaxHeight()
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                                    .padding(8.dp)
                            ) {
                                Title("服务器参数设置")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InputView(
                                        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                                        input = hmServerConnectAddress,
                                        hint = "连接地址"
                                    ) {
                                        hmServerConnectAddress.value = it
                                        hmVM.setDownloadData(hmServerConnectAddress.value, hmServerPackageName.value, hmServerVersionName.value) {
                                            serverHmHapFile.clear()
                                            serverHmHapFile.add(it)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InputView(
                                        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                                        input = hmServerPackageName,
                                        hint = "包名"
                                    ) {
                                        hmServerPackageName.value = it
                                        hmVM.setDownloadData(hmServerConnectAddress.value, hmServerPackageName.value, hmServerVersionName.value) {
                                            serverHmHapFile.clear()
                                            serverHmHapFile.add(it)
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    InputView(
                                        modifier = Modifier.padding(end = 16.dp, bottom = 16.dp),
                                        input = hmServerVersionName,
                                        hint = "版本号"
                                    ) {
                                        hmServerVersionName.value = it
                                        hmVM.setDownloadData(hmServerConnectAddress.value, hmServerPackageName.value, hmServerVersionName.value) {
                                            serverHmHapFile.clear()
                                            serverHmHapFile.add(it)
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f).fillMaxHeight().padding(0.dp, 8.dp, 0.dp, 0.dp)
                                    .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor)
                            ) {
                                Title("服务器包", rightIcon = Icons.Default.Refresh, rightContentDescription = "刷新") {
                                    if (hmServerConnectAddress.value.isEmpty()) {
                                        println("请输入服务器地址")
                                        return@Title
                                    }
                                    if (hmServerPackageName.value.isEmpty()) {
                                        println("请输入鸿蒙包名")
                                        return@Title
                                    }
                                    if (hmServerVersionName.value.isEmpty()) {
                                        println("请输入鸿蒙版本号")
                                        return@Title
                                    }
                                    hmVM.getNewHmHap(
                                        hmServerConnectAddress.value,
                                        hmServerPackageName.value,
                                        hmServerVersionName.value,
                                        forceDownload = true
                                    ) { fileInfo ->
                                        serverHmHapFile.clear()
                                        serverHmHapFile.add(fileInfo)
                                    }
                                }
                                LazyColumn {
                                    items(serverHmHapFile) { hapFileInfo ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                                .clip(shape = RoundedCornerShape(8.dp))
                                                .background(color = Color.White),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                "文件名:" + hapFileInfo!!.hapFile.name,
                                                color = Color.Black,
                                                fontSize = 13.sp,
                                                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp).fillMaxWidth()
                                            )
                                            Text(
                                                "上传服务器时间:" + hapFileInfo.uploadServerTime.formatToSimpleData(),
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp).fillMaxWidth()
                                            )
                                            Text(
                                                "下载时间:" + hapFileInfo.hapFile.lastModified().formatToSimpleData(),
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp).fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(2f).fillMaxHeight().padding(8.dp, 0.dp, 0.dp, 0.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Title("操作")
                        Button(modifier = Modifier, onClick = {
                            hmVM.clearDownloadCache()
                        }) {
                            Text("清除下载记录")
                        }
                        Button(modifier = Modifier, onClick = {
                            HDCUtil.changeToUsb()
                        }) {
                            Text("切换到USB连接")
                        }
                        Button(modifier = Modifier, onClick = {
                            HDCUtil.changeToPort()
                        }) {
                            Text("切换到网络连接")
                        }
                        Button(modifier = Modifier, onClick = {
                            FileSelector.showFileSelector(suffixList = arrayOf("hap")) { filePath ->
                                val hapFile = File(filePath)
                                val hapInfo = HMUnpackingUtil.getHapInfo(hapFile)
                                hapInfo?.let {
                                    HDCUtil.installHap(hapFile, hapInfo.bundleName ?: "", hapInfo.abilityName ?: "")
                                }
                            }
                        }) {
                            Text("安装本地应用")
                        }
                        Button(modifier = Modifier, onClick = {
                            serverHmHapFile.firstOrNull()?.let { hapFileInfo ->
                                val hapInfo = HMUnpackingUtil.getHapInfo(hapFileInfo.hapFile)
                                hapInfo?.let {
                                    hmClientList.forEach {
                                        HDCUtil.installHap(
                                            hapFileInfo.hapFile,
                                            hapInfo.bundleName ?: "",
                                            hapInfo.abilityName ?: "",
                                            connectKey = it.connectInfo
                                        )
                                    }
                                }
                            }
                        }) {
                            Text("安装服务器最新应用")
                        }
                    }
                }
            }
        }
    }
}