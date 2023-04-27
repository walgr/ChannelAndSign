package com.wpf.util.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposeWindow

@Preview
@Composable
fun AppPreview(window: ComposeWindow) {
    MainView(window)
}