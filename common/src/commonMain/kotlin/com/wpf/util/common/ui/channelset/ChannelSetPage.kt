package com.wpf.util.common.ui.channelset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.utils.DropBoxPanel
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.signset.SignFile
import com.wpf.util.common.ui.signset.SignSetViewModel
import com.wpf.util.common.ui.utils.FileSelector
import javax.swing.JFileChooser

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun channelPage(window: ComposeWindow) {

    //分组列表
    val channelList = remember { mutableStateListOf(*ChannelSetViewModel.getChannelList().toTypedArray()) }
    //渠道包文件名
    val channelFileNameList = remember { mutableStateListOf("") }
    //渠道名
    val channelNameList = remember { mutableStateListOf("") }

    //待打渠道包的apk
    val pathList = remember { mutableStateListOf<Path>() }

    //分组添加Dialog
    val groupDialog = remember { mutableStateOf(false) }
    val groupDialogInput = remember { mutableStateOf("") }

    //签名列表
    val signList = remember { mutableStateListOf(*SignSetViewModel.getSignList().toTypedArray()) }
    //选择签名弹窗
    val showSignSelectDialog = remember { mutableStateOf(false) }

    var isRunDealFile by remember { mutableStateOf(false) }

    channelList.find { channel -> channel.isSelect }?.channelPath?.let {
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
                        Text("打渠道包", fontWeight = FontWeight.Bold, color = mainTextColor)
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
                                            "分组",
                                            fontWeight = FontWeight.Bold,
                                            color = mainTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd
                                        ) {
                                            IconButton(onClick = {
                                                groupDialog.value = true
                                            }) {
                                                Icon(
                                                    Icons.Default.AddCircle, "添加分组", modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    LazyColumn {
                                        items(channelList) { group ->
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(44.dp)
                                                    .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                                    .clip(shape = RoundedCornerShape(8.dp))
                                                    .background(color = if (group.isSelectState.value) mainTextColor else Color.White)
                                                    .combinedClickable(
                                                        enabled = true,
                                                        onDoubleClick = {
                                                            groupDialogInput.value = group.name
                                                            groupDialog.value = true
                                                        },
                                                        onLongClick = {
                                                            groupDialogInput.value = group.name
                                                            groupDialog.value = true
                                                        },
                                                        onClick = {
                                                            channelList.forEach {
                                                                it.isSelect = false
                                                                it.isSelectState.value = false
                                                            }
                                                            group.isSelect = true
                                                            group.isSelectState.value = true
                                                            ChannelSetViewModel.saveChannelList(channelList)
                                                            channelFileNameList.clear()
                                                            channelNameList.clear()
                                                            if (group.channelPath.isNotEmpty()) {
                                                                val result =
                                                                    ChannelSetViewModel.getChannelDataInFile(group.channelPath)
                                                                if (result.isNotEmpty()) {
                                                                    channelFileNameList.addAll(result.map { array -> array[0] })
                                                                    channelNameList.addAll(result.map { array -> array[1] })
                                                                } else {
                                                                    channelFileNameList.add("文件解析错误，路径:(${group.channelPath})")
                                                                    channelNameList.add("文件解析错误，路径:(${group.channelPath}")
                                                                }
                                                            }
                                                        }
                                                    ), contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    group.nameState.value,
                                                    color = if (group.isSelectState.value) Color.White else Color.Black,
                                                    modifier = Modifier.padding(8.dp, 4.dp, 8.dp, 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Box(
                                modifier = Modifier.weight(2f).fillMaxHeight().padding(5.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(2f)
                                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "渠道列表",
                                                    fontWeight = FontWeight.Bold,
                                                    color = mainTextColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    IconButton(onClick = {
                                                        FileSelector.showFileSelector(arrayOf("txt"), selectionMode = JFileChooser.FILES_AND_DIRECTORIES) { path ->
                                                            channelList.find { it.isSelect }?.channelPath = path
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
                                                    }) {
                                                        Icon(
                                                            Icons.Default.AddCircle,
                                                            "导入渠道",
                                                            modifier = Modifier.size(18.dp)
                                                        )
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
                                        DropBoxPanel(modifier = Modifier.fillMaxSize(), window = window) {
                                            if (it.size != 1 || !it[0].contains(".txt")) return@DropBoxPanel
                                            channelList.find { channel -> channel.isSelect }?.channelPath = it[0]
                                            channelFileNameList.clear()
                                            channelNameList.clear()
                                            if (it[0].isNotEmpty()) {
                                                val result = ChannelSetViewModel.getChannelDataInFile(it[0])
                                                if (result.isNotEmpty()) {
                                                    channelFileNameList.addAll(result.map { array -> array[0] })
                                                    channelNameList.addAll(result.map { array -> array[1] })
                                                } else {
                                                    channelFileNameList.add("文件解析错误，路径:(${it[0]})")
                                                    channelNameList.add("文件解析错误，路径:(${it[0]}")
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f).padding(0.dp, 10.dp, 0.dp, 0.dp)
                                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                                    ) {
                                        Column {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "apk地址",
                                                    fontWeight = FontWeight.Bold,
                                                    color = mainTextColor,
                                                    textAlign = TextAlign.Center
                                                )
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.CenterEnd
                                                ) {
                                                    IconButton(onClick = {
                                                        FileSelector.showFileSelector(arrayOf("apk"), selectionMode = JFileChooser.FILES_AND_DIRECTORIES) {
                                                            pathList.add(Path(name = it, path = it))
                                                        }
                                                    }) {
                                                        Icon(
                                                            Icons.Default.AddCircle,
                                                            "添加Apk",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Box(modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 8.dp)) {
                                                Column {
                                                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                                        Text(
                                                            "选择完apk后，后续生成的相关渠道包会保存在apk同级目录下",
                                                            fontSize = 10.sp, color = Color.Gray
                                                        )
                                                    }
                                                    LazyColumn {
                                                        items(pathList) {
                                                            Text(it.path,
                                                                fontSize = 11.sp,
                                                                color = Color.DarkGray,
                                                                modifier = Modifier.combinedClickable(
                                                                    onDoubleClick = {
                                                                        pathList.remove(it)
                                                                    }
                                                                ) {

                                                                })
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        DropBoxPanel(modifier = Modifier.fillMaxSize(), window = window) {
//                                            if (it.size != 1) return@DropBoxPanel
//                                            pathList.clear()
                                            pathList.addAll(it.flatMap { file ->
                                                //apk或文件夹
                                                if (file.contains(".apk") || !file.contains(".")) {
                                                    mutableListOf(Path(name = file, path = file))
                                                } else mutableListOf()
                                            }.toMutableList())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.width(150.dp).fillMaxHeight().padding(0.dp, 0.dp, 0.dp, 16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = {
                    if (pathList.isEmpty()) {

                    } else {
                        if (signList.size == 1) {
                            isRunDealFile = true
                            ChannelSetViewModel.dealApk(pathList.map {
                                it.path
                            }, channelList.find { it.isSelect }?.channelPath, signList[0]) {
                                isRunDealFile = false
                            }
                        } else {
                            showSignSelectDialog.value = true
                        }
                    }
                }) {
                    Icon(if (isRunDealFile) Icons.Default.Close else Icons.Default.PlayArrow, "开始打包")
                }
            }
        }
        if (groupDialog.value) {
            showGroupAddDialog(groupDialog, groupDialogInput) { inputStr ->
                channelList.forEach {
                    it.isSelect = false
                    it.isSelectState.value = false
                }
                val findGroup = channelList.find {
                    it.name == groupDialogInput.value
                }
                if (findGroup != null) {
                    findGroup.name = inputStr
                    findGroup.nameState.value = inputStr
                } else {
                    channelList.add(Channel(inputStr).apply {
                        isSelect = true
                        isSelectState.value = true
                    })
                }
                ChannelSetViewModel.saveChannelList(channelList)
                groupDialogInput.value = ""
            }
        }
        if (showSignSelectDialog.value) {
            showSelectSignDialog(showSignSelectDialog) {
                isRunDealFile = true
                ChannelSetViewModel.dealApk(
                    pathList.map { path -> path.path },
                    channelList.find { channel -> channel.isSelect }?.channelPath,
                    it
                ) {
                    isRunDealFile = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun showSelectSignDialog(showSignSelectDialog: MutableState<Boolean>, callback: (SignFile) -> Unit) {
    //签名列表
    val signList = remember { mutableStateListOf(*SignSetViewModel.getSignList().toTypedArray()) }
    signList.getOrNull(0)?.isSelect = true

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
                signList.forEach { sign ->
                    Row(modifier = Modifier.weight(1f).padding(start = 8.dp).clickable {
                        signList.forEach {
                            it.isSelect = false
                            it.isSelectState.value = false
                        }
                        sign.isSelect = true
                        sign.isSelectState.value = true
                    }) {
                        RadioButton(
                            selected = sign.isSelectState.value,
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

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun showGroupAddDialog(
    groupDialog: MutableState<Boolean>,
    groupDialogInput: MutableState<String>,
    callback: ((String) -> Unit)
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