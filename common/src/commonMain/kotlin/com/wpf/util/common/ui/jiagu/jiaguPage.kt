package com.wpf.util.common.ui.jiagu

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
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
import com.wpf.util.common.ui.utils.*
import com.wpf.util.common.ui.widget.common.InputView
import com.wpf.util.common.ui.widget.common.ItemView
import com.wpf.util.common.ui.widget.common.SelectFileAddTitle
import com.wpf.util.common.ui.widget.common.Title
import com.wpf.utils.jiagu.JiaGu
import com.wpf.utils.jiagu.utils.AES128Helper
import com.wpf.utils.jiagu.utils.AES128Helper.KEY_VI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser


@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun jiaguPage() {
    var isRunDealFile by remember { mutableStateOf(false) }
    val dealFileList by autoSaveListComposable("jiaguDealFileList") { remember { mutableStateListOf<Path>() } }

    val inputAes128Key by autoSaveComposable("inputAes128Key") { remember { mutableStateOf("") } }
    val inputAes128KeyIV by autoSaveComposable("inputAes128KeyIV") { remember { mutableStateOf("") } }
    val inputAndroidSdkPath by autoSaveComposable("inputAndroidSdkPath") { remember { mutableStateOf("") } }

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
                        Text("加固", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Row {
                        Box(
                            modifier = Modifier.weight(2f).fillMaxHeight()
                                .padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
                                .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier.wrapContentWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Title("密钥设置", modifier = Modifier.wrapContentWidth().height(44.dp))
                                }
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        InputView(
                                            modifier = Modifier.weight(1f),
                                            input = inputAes128Key,
                                            hint = "请输入密钥"
                                        ) {
                                            inputAes128Key.value = it
                                            AES128Helper.DEFAULT_SECRET_KEY = it
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        InputView(
                                            modifier = Modifier.weight(1f),
                                            input = inputAes128KeyIV,
                                            hint = "请输入密钥IV"
                                        ) {
                                            inputAes128KeyIV.value = it
                                            AES128Helper.KEY_VI = it
                                        }
                                    }
                                }
                                Button(onClick = {
                                    AES128Helper.DEFAULT_SECRET_KEY = AES128Helper.getRandom(16)
                                    KEY_VI = AES128Helper.getRandom(16)
                                    inputAes128Key.value = AES128Helper.DEFAULT_SECRET_KEY
                                    inputAes128KeyIV.value = KEY_VI
                                }, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("生成新密钥IV")
                                }

                                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(top = 16.dp)) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Row(
                                            modifier = Modifier.wrapContentWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Title("加固打包设置", modifier = Modifier.wrapContentWidth().height(44.dp))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
//                                            Row {
                                                InputView(
                                                    modifier = Modifier.weight(1f),
                                                    input = inputAndroidSdkPath,
                                                    hint = "请输入Android SDK"
                                                ) {
                                                    inputAndroidSdkPath.value = it
                                                }
                                                Button(onClick = {
                                                    FileSelector.showFileSelector(selectionMode = JFileChooser.DIRECTORIES_ONLY) {
                                                        inputAndroidSdkPath.value = it
                                                    }
                                                }, modifier = Modifier.padding(start = 8.dp)) {
                                                    Text("选择")
                                                }
//                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .padding(start = 8.dp, end = 16.dp, bottom = 16.dp)
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
                                                        runCatching {
                                                            JiaGu.deal(
                                                                it,
                                                                secretKey = inputAes128Key.value,
                                                                keyVi = inputAes128KeyIV.value,
                                                                androidSdkPath = inputAndroidSdkPath.value,
                                                                showLog = true
                                                            )
                                                        }.onFailure {
                                                            println("加固失败：${it.message}")
                                                        }
                                                    }
                                                    isRunDealFile = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                if (isRunDealFile) Icons.Default.Close else Icons.Default.PlayArrow,
                                                "开始加固"
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
        }
    }
}