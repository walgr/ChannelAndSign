package com.wpf.util.common.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

val itemBgColor = Color(1f, 1f, 1f, 0.6f)
val mainTextColor = Color(98, 88, 179)
val mainBgColor = Color(28, 30, 46)
val centerBgColor = Color(234, 234, 245)

@Composable
fun closeIcon() = Icon(Icons.Default.Close, "失败")

@Composable
fun size24(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.size(24.dp), content = content
    )
}
@Composable
fun size44(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier.size(44.dp), content = content
    )
}

@Composable
fun icon() = size44 {
    Image(
        painterResource("/icon.png"), contentDescription = "icon", modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun apiIcon() = size24 {
    Image(
        painter = painterResource("/image/api.png"), contentDescription = "api上传", modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun uploadIcon() = size24 {
    Image(
        painter = painterResource("/image/upload.png"),
        contentDescription = "上传市场",
        modifier = Modifier.fillMaxSize()
    )
}