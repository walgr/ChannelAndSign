package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

@Preview
@Composable
fun InputView(input: MutableState<String> = mutableStateOf(""), hint: String = "", modifier: Modifier = Modifier, onTextChange: (String) -> Unit) {
    val showText = remember { input }
    OutlinedTextField(value = TextFieldValue(
        showText.value, TextRange(showText.value.length)
    ), onValueChange = {
        showText.value = it.text
        onTextChange.invoke(it.text)
    }, label = {
        Text(hint)
    }, singleLine = true, modifier = Modifier.fillMaxWidth().then(modifier)
    )
}