package com.wpf.util.common.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.DragData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.onExternalDrag

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun onExternalDrag(
    modifier: Modifier = Modifier, onFileDrop: (List<String>) -> Unit
) {
    Box(modifier = modifier.fillMaxSize().onExternalDrag(onDrop = {
        if (it.dragData is DragData.FilesList) {
            onFileDrop.invoke((it.dragData as DragData.FilesList).readFiles().map { file ->
                file.replace("file:/", "")
            })
        }
    }))
}