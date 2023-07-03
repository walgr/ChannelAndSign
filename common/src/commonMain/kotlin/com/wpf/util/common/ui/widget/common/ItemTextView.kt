package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wpf.util.common.ui.mainTextColor

@Preview
@Composable
fun ItemTextView(
    text: String,
    isSelectState: MutableState<Boolean> = mutableStateOf(false),
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val isSelect = remember { isSelectState }
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(start = 8.dp, top = 4.dp, end = 8.dp, bottom = 4.dp).clip(shape = RoundedCornerShape(8.dp))
            .background(color = if (isSelect.value) mainTextColor else Color.White)
            .then(modifier), contentAlignment = Alignment.CenterStart, content = content ?: {
            Text(
                text = text,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                color = if (isSelect.value) Color.White else Color.Black,
                fontSize = 14.sp
            )
        }
    )
}