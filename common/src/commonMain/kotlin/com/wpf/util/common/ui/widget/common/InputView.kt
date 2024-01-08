package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation

@Preview
@Composable
fun InputView(
    modifier: Modifier? = null,
    input: MutableState<String> = mutableStateOf(""),
    hint: String = "",
    maxLine: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onTextChange: (String) -> Unit
) {
    val showText = remember { input }
    OutlinedTextField(
        value = showText.value,
        onValueChange = {
            showText.value = it
            onTextChange.invoke(it)
        },
        label = {
            Text(hint)
        },
        singleLine = maxLine == 1,
        maxLines = maxLine,
        trailingIcon = {
            if (showText.value.isNotEmpty()) {
                Icon(
                    Icons.Rounded.Clear,
                    "清空",
                    modifier = Modifier.clickable {
                        showText.value = ""
                        onTextChange.invoke("")
                    }
                )
            }
        },
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = if (modifier == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().then(modifier)
    )
}