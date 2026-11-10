package com.wpf.util.common.ui.jiagu

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
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
import com.wpf.utils.jiagu.XapkArchive
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
    val resourcePaths by autoSaveComposable("jiaguResourcePaths") { remember { mutableStateOf(false) } }
    val resourceContents by autoSaveComposable("jiaguResourceContents") { remember { mutableStateOf(false) } }
    val nativeContents by autoSaveComposable("jiaguNativeContents") { remember { mutableStateOf(false) } }
    val nativeExclusions by autoSaveComposable("jiaguNativeExclusions") { remember { mutableStateOf("") } }
    val settingsScrollState = rememberScrollState()

    Box {
        Row {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().clip(shape = RoundedCornerShape(8.dp))
                    .background(color = itemBgColor)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(all = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加固", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Box(
                            modifier = Modifier.weight(2f).fillMaxHeight()
                                .padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
                                .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(settingsScrollState)
                                    .padding(end = 12.dp, bottom = 16.dp),
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = resourcePaths.value, enabled = !isRunDealFile,
                                                onCheckedChange = { resourcePaths.value = it })
                                            Text("资源路径混淆", color = mainTextColor)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = resourceContents.value, enabled = !isRunDealFile,
                                                onCheckedChange = { resourceContents.value = it })
                                            Text("轻量资源内容加密（图片、布局、assets）", color = mainTextColor)
                                        }
                                        Text("单文件最多 256 KB、总计 8 MB；保留启动资源和大文件，复用解密缓存", fontSize = 11.sp,
                                            color = mainTextColor, modifier = Modifier.padding(horizontal = 16.dp))
                                        Text("资源完整性校验默认开启；混淆保留资源 ID 和 assets 路径", fontSize = 11.sp,
                                            color = mainTextColor, modifier = Modifier.padding(horizontal = 16.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = nativeContents.value, enabled = !isRunDealFile,
                                                onCheckedChange = { nativeContents.value = it })
                                            Text("轻量 SO 内容加密", color = mainTextColor)
                                        }
                                        Text("SO 完整性默认开启；只加密入口库，单库 4 MB / 每 ABI 16 MB，复用解密缓存；XAPK 仅主包", fontSize = 11.sp,
                                            color = mainTextColor, modifier = Modifier.padding(horizontal = 16.dp))
                                        InputView(input = nativeExclusions, hint = "SO 加密排除：libxxx.so，多个用英文逗号分隔") {
                                            nativeExclusions.value = it
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
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(settingsScrollState),
                                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                                    .padding(vertical = 8.dp, horizontal = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight()
                                .padding(start = 8.dp, end = 16.dp, bottom = 16.dp)
                                .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                        ) {
                            Column {
                                SelectFileAddTitle("文件列表", arrayOf("apk", "xapk")) {
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
                                                    if (XapkArchive.isPackage(pathFile)) {
                                                        listOf(it.path)
                                                    } else if (pathFile.isDirectory) {
                                                        pathFile.listFiles { child -> XapkArchive.isPackage(child) }
                                                            ?.map { child ->
                                                                child.path
                                                            }?.toList() ?: emptyList()
                                                    } else {
                                                        emptyList()
                                                    }
                                                }
                                                if (apkPathList.isEmpty()) { isRunDealFile = false; return@IconButton }
                                                CoroutineScope(Dispatchers.Default).launch {
                                                    apkPathList.forEach {
                                                        runCatching {
                                                            JiaGu.deal(
                                                                it,
                                                                secretKey = inputAes128Key.value,
                                                                keyVi = inputAes128KeyIV.value,
                                                                androidSdkPath = inputAndroidSdkPath.value,
                                                                showLog = true,
                                                                obfuscateResourcePaths = resourcePaths.value,
                                                                encryptResourceContents = resourceContents.value,
                                                                encryptNativeLibraries = nativeContents.value,
                                                                nativeExclusions = nativeExclusions.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
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
                                    if (File(file).isDirectory || XapkArchive.isPackage(File(file))) {
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
