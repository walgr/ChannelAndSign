package com.wpf.util.common.ui.channelset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.marketplace.markets.base.UploadData
import com.wpf.util.common.ui.marketplace.markets.base.upload
import com.wpf.util.common.ui.signset.SignFile
import com.wpf.util.common.ui.uploadIcon
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.common.*
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun channelPage() {
    //分组列表
    val clientList by autoSaveListComposable("clientList") { remember { mutableStateListOf<Client>() } }
    //渠道包文件名
    val channelFileNameList = remember { mutableStateListOf("") }
    //渠道名
    val channelNameList = remember { mutableStateListOf("") }

    //待打渠道包的apk
    val pathList by autoSaveListComposable("pathList") { remember { mutableStateListOf<Path>() } }

    //分组添加Dialog
    val groupDialog = remember { mutableStateOf(false) }
    val groupDialogInput = remember { mutableStateOf("") }

    //签名列表
    val signList by autoSaveListComposable("signList") { remember { mutableStateListOf<SignFile>() } }
    //选择签名弹窗
    val showSignSelectDialog = remember { mutableStateOf(false) }

    var isRunDealFile by remember { mutableStateOf(false) }

    val marketDescription = autoSaveComposable("marketDescription") { remember { mutableStateOf("") } }
    val marketLeaveMessage = autoSaveComposable("marketLeaveMessage") { remember { mutableStateOf("") } }
    val marketScreenShotList by autoSaveListComposable("marketScreenShotList") { remember { mutableStateListOf("") } }
    //打完后的市场包列表
    val marketPlaceList =
        remember { mutableStateListOf(*ChannelSetViewModel.dealMargetPlace(pathList.map { it.path }).toTypedArray()) }

    clientList.find { client -> client.isSelect }?.channelPath?.let {
        if (it.isNotEmpty()) {
            val result = ChannelSetViewModel.getChannelDataInFile(it)
            result.isNotEmpty().let {
                channelFileNameList.clear()
                channelFileNameList.addAll(result.map { array -> array[0] })
                channelNameList.clear()
                channelNameList.addAll(result.map { array -> array[1] })
            }
        }
    }

    Box(
        modifier = Modifier.background(color = itemBgColor)
    ) {
        Row {
            Box(
                modifier = Modifier.weight(2f).fillMaxHeight().clip(shape = RoundedCornerShape(8.dp))
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(all = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("打渠道包", fontWeight = FontWeight.Bold, color = mainTextColor)
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
                                    Title("客户端") {
                                        groupDialog.value = true
                                    }
                                    LazyColumn {
                                        items(clientList) { group ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                                    .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                                    .clip(shape = RoundedCornerShape(8.dp))
                                                    .background(color = if (group.isSelectState.value) mainTextColor else Color.White)
                                                    .combinedClickable(enabled = true, onLongClick = {
                                                        //长按 修改
                                                        groupDialogInput.value = group.name
                                                        groupDialog.value = true
                                                    }, onClick = {
                                                        dealClientClick(
                                                            clientList,
                                                            clientList.indexOf(group),
                                                            channelFileNameList,
                                                            channelNameList
                                                        )
                                                    }), contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Text(
                                                    group.nameState.value,
                                                    color = if (group.isSelectState.value) Color.White else Color.Black,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp).fillMaxWidth()
                                                )
                                                IconButton(onClick = {
                                                    clientList.remove(group)
                                                    dealClientClick(clientList, 0, channelFileNameList, channelNameList)
                                                }) { Icon(Icons.Default.Close, "关闭") }
                                            }
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.weight(2f).fillMaxHeight().padding(4.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f)
                                            .clip(shape = RoundedCornerShape(8.dp))
                                            .background(color = centerBgColor),
                                    ) {
                                        Column {
                                            SelectFileAddTitle("渠道文件", arrayOf("txt")) { path ->
                                                clientList.find { it.isSelect }?.channelPath = path
                                                clientList.saveData()
                                                channelFileNameList.clear()
                                                channelNameList.clear()
                                                if (path.isNotEmpty()) {
                                                    val result = ChannelSetViewModel.getChannelDataInFile(path)
                                                    if (result.isNotEmpty()) {
                                                        channelFileNameList.addAll(result.map { array -> array[0] })
                                                        channelNameList.addAll(result.map { array -> array[1] })
                                                    } else {
                                                        channelFileNameList.add("文件解析错误，路径:(${path})")
                                                        channelNameList.add("文件解析错误，路径:(${path}")
                                                    }
                                                }
                                            }
                                            Box(modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 8.dp)) {
                                                Row {
                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(0.dp, 0.dp, 0.dp, 4.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text("渠道包文件名", fontSize = 10.sp)
                                                        }
                                                        LazyColumn {
                                                            items(channelFileNameList) {
                                                                Text(it, fontSize = 11.sp, color = Color.DarkGray)
                                                            }
                                                        }
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier.fillMaxWidth()
                                                                .padding(0.dp, 0.dp, 0.dp, 4.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text("渠道名", fontSize = 10.sp)
                                                        }
                                                        LazyColumn {
                                                            items(channelNameList) {
                                                                Text(it, fontSize = 11.sp, color = Color.DarkGray)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        onExternalDrag(modifier = Modifier.fillMaxSize()) {
                                            if (it.size != 1 || !it[0].contains(".txt")) return@onExternalDrag
                                            val realPath = it[0].checkWinPath()
                                            clientList.find { client -> client.isSelect }?.channelPath = realPath
                                            clientList.saveData()
                                            channelFileNameList.clear()
                                            channelNameList.clear()
                                            if (realPath.isNotEmpty()) {
                                                val result = ChannelSetViewModel.getChannelDataInFile(realPath)
                                                if (result.isNotEmpty()) {
                                                    channelFileNameList.addAll(result.map { array -> array[0] })
                                                    channelNameList.addAll(result.map { array -> array[1] })
                                                } else {
                                                    channelFileNameList.add("文件解析错误，路径:(${realPath})")
                                                    channelNameList.add("文件解析错误，路径:(${realPath}")
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(4f)
                                            .padding(0.dp, 10.dp, 0.dp, 0.dp)
                                            .clip(shape = RoundedCornerShape(8.dp))
                                            .background(color = centerBgColor),
                                    ) {
                                        Column {
                                            SelectFileAddTitle("打渠道包并签名", arrayOf("apk")) {
                                                pathList.add(Path(name = it, path = it))
                                            }
                                            Box(modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp)) {
                                                Column {
                                                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                                        Text(
                                                            "选择完apk后，渠道包默认保存在apk同级目录下或者在软件配置里配置路径",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        items(pathList) {
                                                            ItemView(modifier = Modifier.heightIn(min = 24.dp)
                                                                .combinedClickable(onDoubleClick = {
                                                                    pathList.remove(it)
                                                                }) {}) {
                                                                Text(
                                                                    it.path,
                                                                    fontSize = 11.sp,
                                                                    color = Color.DarkGray,
                                                                    modifier = Modifier.padding(
                                                                        start = 8.dp, end = 8.dp
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        onExternalDrag {
                                            pathList.addAll(it.flatMap { file ->
                                                //apk或文件夹
                                                if (file.contains(".apk") || !file.contains(".")) {
                                                    mutableListOf(Path(name = file, path = file))
                                                } else mutableListOf()
                                            }.toMutableList())
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.BottomEnd
                                        ) {
                                            IconButton(onClick = {
                                                if (pathList.isEmpty()) {

                                                } else {
                                                    if (signList.size == 1) {
                                                        isRunDealFile = true
                                                        val filePathList = pathList.map {
                                                            it.path
                                                        }
                                                        ChannelSetViewModel.dealApk(
                                                            filePathList,
                                                            clientList.find { it.isSelect }?.channelPath,
                                                            signList[0]
                                                        ) {
                                                            isRunDealFile = false
                                                            marketPlaceList.clear()
                                                            marketPlaceList.addAll(
                                                                ChannelSetViewModel.dealMargetPlace(
                                                                    filePathList
                                                                )
                                                            )
                                                        }
                                                    } else {
                                                        showSignSelectDialog.value = true
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    if (isRunDealFile) Icons.Default.Close else Icons.Default.PlayArrow,
                                                    "开始打包"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 0.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Title("市场包")
                            InputView(input = marketDescription, hint = "请输入更新文案", maxLine = 5) {
                                marketDescription.value = it
                            }
                            InputView(input = marketLeaveMessage, hint = "请输入留言", maxLine = 5) {
                                marketLeaveMessage.value = it
                            }
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {
                                items(marketScreenShotList) {
                                    AddImage(it) { new ->
                                        marketScreenShotList.remove(it)
                                        if (new.isNotEmpty()) {
                                            marketScreenShotList.add(new)
                                            if (marketScreenShotList.size < 5) {
                                                marketScreenShotList.add("")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(shape = RoundedCornerShape(8.dp))
                            .background(color = centerBgColor).padding(8.dp),
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(marketPlaceList) {
                                ApkItem(it)
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd
                ) {
                    IconButton(onClick = {
                        marketPlaceList.filter {
                            it.isSelectState.value
                        }.map { marketApk ->
                            UploadData(marketApk,
                                marketDescription.value,
                                marketLeaveMessage.value.ifEmpty { null },
                                marketScreenShotList.value.filter { screenShot ->
                                    screenShot.isNotEmpty()
                                })
                        }.forEach {
                            it.upload()
                        }
                    }) {
                        uploadIcon()
                    }
                }
            }
        }
        if (groupDialog.value) {
            showGroupAddDialog(groupDialog, groupDialogInput) { inputStr ->
                clientList.forEach {
                    it.isSelect = false
                    it.isSelectState.value = false
                }
                val findGroup = clientList.find {
                    it.name == groupDialogInput.value
                }
                if (findGroup != null) {
                    findGroup.name = inputStr
                    findGroup.nameState.value = inputStr
                } else {
                    clientList.add(Client(id = UUID.randomUUID().toString(), inputStr).apply {
                        isSelect = true
                        isSelectState.value = true
                    })
                }
                groupDialogInput.value = ""
            }
        }
        if (showSignSelectDialog.value) {
            showSelectSignDialog(showSignSelectDialog) {
                isRunDealFile = true
                val filePathList = pathList.map { file ->
                    file.path
                }
                ChannelSetViewModel.dealApk(
                    filePathList, clientList.find { client -> client.isSelect }?.channelPath, it
                ) {
                    isRunDealFile = false
                    marketPlaceList.clear()
                    marketPlaceList.addAll(ChannelSetViewModel.dealMargetPlace(filePathList))
                }
            }
        }
    }
}

fun dealClientClick(
    clientList: AutoSaveList<Client, SnapshotStateList<Client>>,
    pos: Int,
    channelFileNameList: SnapshotStateList<String>,
    channelNameList: SnapshotStateList<String>
) {
    clientList.forEach {
        it.isSelect = false
        it.isSelectState.value = false
    }
    val client = clientList[pos]
    client.isSelect = true
    client.isSelectState.value = true
    clientList.saveData()
    channelFileNameList.clear()
    channelNameList.clear()
    if (client.channelPath.isNotEmpty()) {
        val result =
            ChannelSetViewModel.getChannelDataInFile(client.channelPath)
        if (result.isNotEmpty()) {
            channelFileNameList.addAll(result.map { array -> array[0] })
            channelNameList.addAll(result.map { array -> array[1] })
        } else {
            channelFileNameList.add("文件解析错误，路径:(${client.channelPath})")
            channelNameList.add("文件解析错误，路径:(${client.channelPath}")
        }
    }
}

@Preview
@Composable
private fun showSelectSignDialog(showSignSelectDialog: MutableState<Boolean>, callback: (SignFile) -> Unit) {
    //签名列表
    val signList by autoSaveListComposable("signList") { remember { mutableStateListOf<SignFile>() } }
    if (signList.find { it.isSelect } == null) {
        signList.getOrNull(0)?.isSelect = true
        signList.getOrNull(0)?.isSelectState?.value = true
    }

    AlertDialog(onDismissRequest = {
        showSignSelectDialog.value = false
    }, dismissButton = {
        TextButton(onClick = {
            showSignSelectDialog.value = false
        }) {
            Text(text = "取消")
        }
    }, confirmButton = {
        TextButton(onClick = {
            signList.find { it.isSelect }?.let {
                showSignSelectDialog.value = false
                callback.invoke(it)
            }
        }) {
            Text(text = "签名")
        }
    }, title = {
        Text("请选择签名文件")
    }, text = {
        Column {
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                val isSelectRL = signList.map { remember { it.isSelectState } }
                signList.forEach { sign ->
                    Row(modifier = Modifier.weight(1f).padding(start = 8.dp).clickable {
                        signList.forEach {
                            it.isSelect = false
                            it.isSelectState.value = false
                            isSelectRL[signList.indexOf(it)].value = false
                        }
                        sign.isSelect = true
                        sign.isSelectState.value = true
                        isSelectRL[signList.indexOf(sign)].value = true
                        signList.saveData()
                    }) {
                        RadioButton(
                            selected = isSelectRL[signList.indexOf(sign)].value,
                            onClick = null,
                            enabled = true,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(sign.name, modifier = Modifier.padding(start = 8.dp))
                    }

                }
            }

        }
    })
}

@Composable
fun showGroupAddDialog(
    groupDialog: MutableState<Boolean>, groupDialogInput: MutableState<String>, callback: ((String) -> Unit)
) {
    var inputStr by remember { mutableStateOf(groupDialogInput.value) }

    AlertDialog(onDismissRequest = {
        groupDialog.value = false
    }, dismissButton = {
        TextButton(onClick = {
            groupDialog.value = false
        }) {
            Text(text = "取消")
        }
    }, confirmButton = {
        TextButton(onClick = {
            groupDialog.value = false
            callback.invoke(inputStr)
        }) {
            Text(text = "确认")
        }
    }, title = {
        Text(if (groupDialogInput.value.isEmpty()) "添加分组" else "修改分组")
    }, text = {
        Column {
            Text("")
            OutlinedTextField(
                value = TextFieldValue(inputStr, TextRange(inputStr.length)),
                onValueChange = {
                    inputStr = it.text
                },
                placeholder = {
                    Text("请输入分组名称")
                },
                singleLine = true,
            )
        }
    })
}