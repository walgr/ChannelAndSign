package com.wpf.util.common.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.mainTextColor

@Composable
fun ItemView(
    isSelectState: Boolean = false, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp).padding(4.dp).clip(shape = RoundedCornerShape(8.dp))
            .background(color = if (isSelectState) mainTextColor else Color.White)
            .then(modifier), contentAlignment = Alignment.CenterStart, content = content
    )
}