package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.sharp.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun InputView(
    modifier: Modifier? = null,
    input: MutableState<String> = mutableStateOf(""),
    hint: String = "",
    maxLine: Int = 1,
    onTextChange: (String) -> Unit
) {
    val showText = remember { input }
    OutlinedTextField(
        value = TextFieldValue(
            showText.value, TextRange(showText.value.length)
        ),
        onValueChange = {
            showText.value = it.text
            onTextChange.invoke(it.text)
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
        modifier = if (modifier == null) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().then(modifier)
    )
}