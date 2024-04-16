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
import com.wpf.utils.curPath
import com.wpf.utils.ex.createCheck
import com.wpf.utils.jiagu.Jiagu
import com.wpf.utils.jiagu.utils.RSAUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun jiaguPage() {
    var isRunDealFile by remember { mutableStateOf(false) }
    val dealFileList by autoSaveListComposable("jiaguDealFileList") { remember { mutableStateListOf<Path>() } }

    val inputPrivateKeyFilePath by autoSaveComposable("privateKeyFilePath") { remember { mutableStateOf("") } }
    val inputPublicKeyFilePath by autoSaveComposable("publicKeyFilePath") { remember { mutableStateOf("") } }

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
                    Box(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier = Modifier.wrapContentWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Title("公私钥设置", modifier = Modifier.wrapContentWidth().height(44.dp))
                            }
                            Row {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InputView(
                                        modifier = Modifier.weight(1f),
                                        input = inputPrivateKeyFilePath,
                                        hint = "请输入私钥位置"
                                    ) {
                                        inputPrivateKeyFilePath.value = it
                                    }
                                    Button(onClick = {
                                        FileSelector.showFileSelector(arrayOf("key", "cer", "crt")) {
                                            inputPrivateKeyFilePath.value = it
                                        }
                                    }, modifier = Modifier.padding(start = 8.dp)) {
                                        Text("选择")
                                    }
                                }

                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InputView(
                                        modifier = Modifier.weight(1f),
                                        input = inputPublicKeyFilePath,
                                        hint = "请输入公钥位置"
                                    ) {
                                        inputPublicKeyFilePath.value = it
                                    }
                                    Button(onClick = {
                                        FileSelector.showFileSelector(arrayOf("cer", "crt")) {
                                            inputPublicKeyFilePath.value = it
                                        }
                                    }, modifier = Modifier.padding(start = 8.dp)) {
                                        Text("选择")
                                    }
                                }
                            }
                            Button(onClick = {
                                val keyPair = RSAUtil.generateKeyPair()
                                val privateKeyFile =
                                    File(curPath + File.separator + "rsa" + File.separator + "privateKey.key").createCheck(
                                        true
                                    )
                                val publicKeyFile =
                                    File(curPath + File.separator + "rsa" + File.separator + "publicKey.cer").createCheck(
                                        true
                                    )
                                privateKeyFile.writeText(keyPair.privateKey)
                                publicKeyFile.writeText(keyPair.publicKey)
                                inputPrivateKeyFilePath.value = privateKeyFile.path
                                inputPublicKeyFilePath.value = publicKeyFile.path
                            }, modifier = Modifier.padding(start = 8.dp)) {
                                Text("生成新公私钥")
                            }
                        }
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
                                                    Jiagu.deal(
                                                        it,
                                                        privateKeyFilePath = inputPrivateKeyFilePath.value,
                                                        publicKeyFilePath = inputPublicKeyFilePath.value,
                                                        showLog = true
                                                    )
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