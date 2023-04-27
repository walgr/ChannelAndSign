package com.wpf.util.common.ui.signset

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.DropBoxPanel

@Preview
@Composable
fun signPage(window: ComposeWindow) {
    //签名列表
    val signList = remember { mutableStateListOf(*SignSetViewModel.getSignList().toTypedArray()) }

    //填写签名配置弹窗
    val showSignInfoDialog = remember { mutableStateOf(false) }
    var changeSign by remember { mutableStateOf(SignFile()) }

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
                        Text("签名配置", fontWeight = FontWeight.Bold, color = mainTextColor)
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp)
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
                                            .padding(8.dp, 4.dp, 8.dp, 4.dp)
                                            .clip(shape = RoundedCornerShape(8.dp))
                                            .background(color = Color(1f, 1f, 1f, 0.6f))
                                            .clickable {
                                                changeSign = it
                                                showSignInfoDialog.value = true
                                            }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Row {
                                                Text("签名名称", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f))
                                                Text(it.name, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f))
                                            }
                                            Row {
                                                Text("签名文件位置", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f))
                                                Text(it.StoreFile, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f))
                                            }
                                            Row {
                                                Text("签名文件密码", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f))
                                                Text(it.StorePass, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f))
                                            }
                                            Row {
                                                Text("签名别名", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f))
                                                Text(it.KeyAlias, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f))
                                            }
                                            Row {
                                                Text("签名别名密码", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(1f))
                                                Text(it.KeyPass, fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(8.dp, 0.dp, 0.dp, 0.dp).weight(3f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        DropBoxPanel(modifier = Modifier.fillMaxSize(), window = window) {
                            if (it.size != 1) return@DropBoxPanel
                            val file = it[0]
                            if (file.contains(".keystore") || file.contains(".jks")) {
                                changeSign = SignFile(StoreFile = file)
                                showSignInfoDialog.value = true
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.width(150.dp).fillMaxHeight().padding(0.dp, 0.dp, 0.dp, 16.dp),
            ) {

            }
        }
        if (showSignInfoDialog.value) {
            showSignInfoSetDialog(showSignInfoDialog, changeSign) { result ->
                val findSign = signList.find { it.StoreFile == result.StoreFile }
                if (findSign == null) {
                    signList.add(result)
                }
                SignSetViewModel.saveSignList(signList)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun showSignInfoSetDialog(showSignInfoDialog: MutableState<Boolean>, signFile: SignFile, callback: ((SignFile) -> Unit)) {
    val inputName = mutableStateOf(signFile.name)
    val inputStoreFile = mutableStateOf(signFile.StoreFile)
    val inputStorePass = mutableStateOf(signFile.StorePass)
    val inputKeyAlias = mutableStateOf(signFile.KeyAlias)
    val inputKeyPass = mutableStateOf(signFile.KeyPass)

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
                OutlinedTextField(
                    value = TextFieldValue(inputName.value, TextRange(inputName.value.length)),
                    onValueChange = {
                        signFile.name = it.text
                        inputName.value = signFile.name
                    },
                    label = {
                        Text("请输入签名名称")
                    },
                    singleLine = true,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = TextFieldValue(inputStoreFile.value, TextRange(inputStoreFile.value.length)),
                    onValueChange = {
                        signFile.StoreFile = it.text
                        inputStoreFile.value = signFile.StoreFile
                    },
                   label = {
                        Text("请输入签名文件位置")
                    },
                    singleLine = true,
                )
            }
            Row(
               modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = TextFieldValue(inputStorePass.value, TextRange(inputStorePass.value.length)),
                    onValueChange = {
                        signFile.StorePass = it.text
                        inputStorePass.value = signFile.StorePass
                    },
                    label = {
                        Text("请输入签名密码")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = TextFieldValue(inputKeyAlias.value, TextRange(inputKeyAlias.value.length)),
                    onValueChange = {
                        signFile.KeyAlias = it.text
                        inputKeyAlias.value = signFile.KeyAlias
                    },
                    label = {
                        Text("请输入签名别名")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                OutlinedTextField(
                    value = TextFieldValue(inputKeyPass.value, TextRange(inputKeyPass.value.length)),
                    onValueChange = {
                        signFile.KeyPass = it.text
                        inputKeyPass.value = signFile.KeyPass
                    },
                    label = {
                        Text("请输入签名别名密码")
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }
        }
    })
}