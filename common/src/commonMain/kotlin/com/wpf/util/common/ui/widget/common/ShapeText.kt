package com.wpf.util.common.ui.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun ShapeText(
    title: String,
    fontColor: Color = Color.White,
    fontSize: TextUnit = TextUnit.Unspecified,
    bgColor: Color = Color.Unspecified,
    shapeRounded: Dp = 4.dp
) {
    Text(
        title,
        color = fontColor,
        fontSize = fontSize,
        modifier = Modifier.clip(shape = RoundedCornerShape(shapeRounded)).background(color = bgColor)
            .padding(3.dp, 1.dp, 3.dp, 1.dp)
    )
}