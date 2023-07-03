package com.wpf.util.common.ui.widget.common

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wpf.util.common.ui.mainTextColor
import com.wpf.util.common.ui.utils.FileSelector
import javax.swing.JFileChooser


@Preview
@Composable
fun Title(title: String, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = mainTextColor,
            textAlign = TextAlign.Center
        )
        onClick?.let {
            Box(
                modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.AddCircle, "添加", modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

}

@Preview
@Composable
fun FileAddTitle(title: String, fileFilter: Array<String> = arrayOf(), onFileSelected: ((String) -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = mainTextColor,
            textAlign = TextAlign.Center
        )
        onFileSelected?.let {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = {
                    FileSelector.showFileSelector(
                        fileFilter,
                        selectionMode = JFileChooser.FILES_AND_DIRECTORIES, onFileSelected
                    )
                }) {
                    Icon(
                        Icons.Default.AddCircle,
                        "导入",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}