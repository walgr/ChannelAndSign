package com.wpf.util.common.ui.widget.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.centerBgColor
import com.wpf.util.common.ui.mainTextColor

@Composable
fun BigTitle(title: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(100.dp).padding(all = 16.dp)
            .clip(shape = RoundedCornerShape(8.dp)).background(color = centerBgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = mainTextColor)
    }
}