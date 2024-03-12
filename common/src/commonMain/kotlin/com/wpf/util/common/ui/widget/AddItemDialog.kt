package com.wpf.util.common.ui.widget

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue


@Preview
@Composable
fun AddItemDialog(showDialog: MutableState<Boolean> = mutableStateOf(false), inputStr: String = "", callback: (String) -> Unit) {
    val showDialogR = remember { showDialog }
    val groupDialogInput = remember { mutableStateOf(inputStr) }

    if (showDialogR.value) {
        AlertDialog(onDismissRequest = {
            showDialogR.value = false
        }, dismissButton = {
            TextButton(onClick = {
                showDialogR.value = false
            }) {
                Text(text = "取消")
            }
        }, confirmButton = {
            TextButton(onClick = {
                showDialogR.value = false
                callback.invoke(groupDialogInput.value)
            }) {
                Text(text = "确认")
            }
        }, title = {
            Text(if (inputStr.isEmpty()) "添加" else "修改")
        }, text = {
            Column {
                Text("")
                OutlinedTextField(
                    value = TextFieldValue(groupDialogInput.value, TextRange(groupDialogInput.value.length)),
                    onValueChange = {
                        groupDialogInput.value = it.text
                    },
                    placeholder = {
                        Text("请输入")
                    },
                    singleLine = true,
                )
            }
        })
    }
}