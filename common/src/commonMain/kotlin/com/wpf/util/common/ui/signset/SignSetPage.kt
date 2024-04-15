package com.wpf.util.common.ui.signset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.itemBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.FileSelector
import com.wpf.util.common.ui.utils.autoSaveListComposable
import com.wpf.util.common.ui.utils.getValue
import com.wpf.util.common.ui.utils.onExternalDrag
import com.wpf.util.common.ui.widget.common.InputView

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun signPage() {
    //签名列表
    val signList by autoSaveListComposable("signList") { remember { mutableStateListOf<SignFile>() } }

    //填写签名配置弹窗
    val showSignInfoDialog = remember { mutableStateOf(false) }
    var changeSign by remember { mutableStateOf(SignFile()) }

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
                        Text("签名配置", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
                    ) {
                        Column {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "签名列表",
                                    fontWeight = FontWeight.Bold,
                                    color = mainTextColor,
                                    textAlign = TextAlign.Center
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    IconButton(onClick = {
                                        changeSign = SignFile()
                                        showSignInfoDialog.value = true
                                    }) {
                                        Icon(
                                            Icons.Default.AddCircle,
                                            "添加签名",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            LazyColumn {
                                items(signList) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .combinedClickable(onDoubleClick = {
                                                changeSign = it
                                                showSignInfoDialog.value = true
                                            }) {

                                            }
                                            .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                            .clip(shape = RoundedCornerShape(8.dp))
                                            .background(color = itemBgColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Row {
                                                Text(
                                                    "签名名称",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f)
                                                )
                                                Text(
                                                    it.name,
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f)
                                                )
                                            }
                                            Row {
                                                Text(
                                                    "签名文件位置",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f)
                                                )
                                                Text(
                                                    it.storeFile,
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f)
                                                )
                                            }
                                            Row {
                                                Text(
                                                    "签名文件密码",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f)
                                                )
                                                Text(
                                                    it.storePass,
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f)
                                                )
                                            }
                                            Row {
                                                Text(
                                                    "签名别名",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f)
                                                )
                                                Text(
                                                    it.keyAlias,
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f)
                                                )
                                            }
                                            Row {
                                                Text(
                                                    "签名别名密码",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f)
                                                )
                                                Text(
                                                    it.keyPass,
                                                    fontSize = 10.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f)
                                                )
                                            }
                                        }
                                        Icon(Icons.Default.Close, "删除",
                                            modifier = Modifier.align(Alignment.TopEnd)
                                                .onClick {
                                                    signList.remove(it)
                                                    signList.saveData()
                                                })
                                    }
                                }
                            }
                        }
                        onExternalDrag {
                            if (it.size != 1) return@onExternalDrag
                            val file = it[0]
                            if (file.contains(".keystore") || file.contains(".jks")) {
                                changeSign = SignFile(storeFile = file)
                                showSignInfoDialog.value = true
                            }
                        }
                    }
                }
            }
        }
        if (showSignInfoDialog.value) {
            showSignInfoSetDialog(showSignInfoDialog, changeSign) { result ->
                signList.removeIf { it.name == result.name }
                signList.add(result)
            }
        }
    }
}

@Composable
private fun showSignInfoSetDialog(
    showSignInfoDialog: MutableState<Boolean>,
    signFile: SignFile,
    callback: ((SignFile) -> Unit)
) {
    val inputName = mutableStateOf(signFile.name)
    val inputStoreFile = mutableStateOf(signFile.storeFile)
    val inputStorePass = mutableStateOf(signFile.storePass)
    val inputKeyAlias = mutableStateOf(signFile.keyAlias)
    val inputKeyPass = mutableStateOf(signFile.keyPass)

    AlertDialog(onDismissRequest = {
        showSignInfoDialog.value = false
    }, dismissButton = {
        TextButton(onClick = {
            showSignInfoDialog.value = false
        }) {
            Text(text = "取消")
        }
    }, confirmButton = {
        TextButton(onClick = {
            showSignInfoDialog.value = false
            callback.invoke(signFile)
        }) {
            Text(text = "确认")
        }
    }, title = {
        Text(if (signFile.name.isEmpty()) "添加签名" else "修改签名")
    }, text = {
        Column(
            modifier = Modifier.widthIn(20.dp, 280.dp)
        ) {
            Text("")
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                InputView(
                    input = inputName,
                    hint = "请输入签名名称",
                ) {
                    signFile.name = it
                    inputName.value = signFile.name
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    InputView(
                        modifier = Modifier.weight(1f),
                        input = inputStoreFile,
                        hint = "请输入签名文件位置",
                    ) {
                        signFile.storeFile = it
                        inputStoreFile.value = signFile.storeFile
                    }
                    Button(onClick = {
                        FileSelector.showFileSelector(arrayOf("keystore", "jks")) {
                            signFile.storeFile = it
                            inputStoreFile.value = signFile.storeFile
                        }
                    }, modifier = Modifier.padding(start = 8.dp)) {
                        Text("选择")
                    }
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                InputView(
                    input = inputStorePass,
                    hint = "请输入签名密码",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                ) {
                    signFile.storePass = it
                    inputStorePass.value = signFile.storePass
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                InputView(
                    input = inputKeyAlias,
                    hint = "请输入签名别名"
                ) {
                    signFile.keyAlias = it
                    inputKeyAlias.value = signFile.keyAlias
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                InputView(
                    input = inputKeyPass,
                    hint = "请输入签名别名密码",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                ) {
                    signFile.keyPass = it
                    inputKeyPass.value = signFile.keyPass
                }
            }
        }
    })
}