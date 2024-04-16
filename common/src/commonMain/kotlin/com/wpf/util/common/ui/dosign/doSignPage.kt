package com.wpf.util.common.ui.dosign

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.channelset.Path
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.signset.SignFile
import com.wpf.util.common.ui.utils.autoSaveListComposable
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.utils.onExternalDrag
import com.wpf.util.common.ui.widget.common.ItemView
import com.wpf.util.common.ui.widget.common.SelectFileAddTitle
import com.wpf.utils.tools.SignHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun doSignPage() {
    var isRunDealFile by remember { mutableStateOf(false) }
    //签名列表
    val signList by autoSaveListComposable("signList") { remember { mutableStateListOf<SignFile>() } }
    val dealFileList by autoSaveListComposable("dealFileList") { remember { mutableStateListOf<Path>() } }

    //选择签名弹窗
    val showSignSelectDialog = remember { mutableStateOf(false) }

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
                        Text("签名打包", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                    ) {
                        Column {
                            SelectFileAddTitle("文件列表", arrayOf("apk")) {
                                dealFileList.add(Path(it, it))
                            }
                            Box(modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 0.dp)) {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(dealFileList) {
                                        ItemView(modifier = Modifier.heightIn(min = 24.dp)
                                            .combinedClickable(onDoubleClick = {
                                                dealFileList.remove(it)
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
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    IconButton(onClick = {
                                        if (isRunDealFile) return@IconButton
                                        if (dealFileList.isEmpty()) {

                                        } else {
                                            if (signList.size == 1) {
                                                isRunDealFile = true
                                                val apkPathList = dealFileList.flatMap {
                                                    val pathFile = File(it.path)
                                                    if (pathFile.isFile && pathFile.extension == "apk") {
                                                        listOf(it.path)
                                                    } else if (pathFile.isDirectory) {
                                                        pathFile.listFiles { child -> pathFile.isFile && child.extension == "apk" }
                                                            ?.map { child ->
                                                                child.path
                                                            }?.toList() ?: emptyList()
                                                    } else {
                                                        emptyList()
                                                    }
                                                }
                                                if (apkPathList.isEmpty()) return@IconButton
                                                CoroutineScope(Dispatchers.Default).launch {
                                                    apkPathList.forEach {
                                                        signList.first().apply {
                                                            SignHelper.sign(storeFile, keyAlias, storePass, keyPass, inputApkPath = it, reserveInput = true)
                                                        }
                                                    }
                                                    isRunDealFile = false
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
                        onExternalDrag {
                            dealFileList.addAll(it.flatMap { file ->
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
        if (showSignSelectDialog.value) {
            showSelectSignDialog(showSignSelectDialog) { sign ->
                isRunDealFile = true
                val apkPathList = dealFileList.flatMap {
                    val pathFile = File(it.path)
                    if (!pathFile.exists()) return@flatMap emptyList()
                    if (pathFile.isFile && pathFile.extension == "apk") {
                        listOf(it.path)
                    } else if (pathFile.isDirectory) {
                        pathFile.listFiles { child -> pathFile.isFile && child.extension == "apk" }
                            ?.map { child ->
                                child.path
                            }?.toList() ?: emptyList()
                    } else {
                        emptyList()
                    }
                }
                if (apkPathList.isEmpty()) return@showSelectSignDialog
                CoroutineScope(Dispatchers.Default).launch {
                    apkPathList.forEach { apkPath ->
                        sign.apply {
                            SignHelper.sign(storeFile, keyAlias, storePass, keyPass, inputApkPath = apkPath, reserveInput = true)
                        }
                    }
                    isRunDealFile = false
                }
            }
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