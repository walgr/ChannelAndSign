package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.utils.Callback
import com.wpf.util.common.ui.utils.FileSelector
import java.io.File

@Preview
@Composable
fun AddImage(imagePath: String, size: DpSize = DpSize(56.dp, 56.dp), callback: (String) -> Unit) {
    val image = remember { mutableStateOf(imagePath) }
    Box(
        modifier = Modifier.size(size)
    ) {
        if (image.value.isNotEmpty()) {
            Image(painter = BitmapPainter(loadImageBitmap(File(image.value).inputStream())),
                "",
                modifier = Modifier.fillMaxSize().clickable {
                    image.value = ""
                    callback.invoke("")
                })
        } else {
            Icon(Icons.Default.Add, "添加", modifier = Modifier.fillMaxSize().clickable {
                FileSelector.showFileSelector(arrayOf("jpg", "png")) {
                    image.value = it
                    callback.invoke(it)
                }
            })
        }
    }
}