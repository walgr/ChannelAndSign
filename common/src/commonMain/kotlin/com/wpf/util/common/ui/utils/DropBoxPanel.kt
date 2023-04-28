package com.wpf.util.common.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import javax.swing.JPanel
import kotlin.math.roundToInt

class DropBoundsBean(
    var x: Int = 0,
    var y: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
)

@Composable
fun DropBoxPanel(
    modifier: Modifier,
    window: ComposeWindow,
    component: JPanel = JPanel(),
    onFileDrop: (List<String>) -> Unit
) {

    val dropBoundsBean = remember {
        mutableStateOf(DropBoundsBean())
    }
    val density = LocalDensity.current.density
    Box(
        modifier = modifier.fillMaxSize().onPlaced {
            dropBoundsBean.value = DropBoundsBean(
                x = (it.positionInWindow().x / density).roundToInt(),
                y = (it.positionInWindow().y / density).roundToInt(),
                width = (it.size.width / density).roundToInt(),
                height = (it.size.height / density).roundToInt()
            )
        }) {
        LaunchedEffect(true) {
            component.setBounds(
                dropBoundsBean.value.x,
                dropBoundsBean.value.y,
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
            component.dropTarget = object : DropTarget() {
                override fun drop(event: DropTargetDropEvent) {

                    event.acceptDrop(DnDConstants.ACTION_REFERENCE)
                    val dataFlavors = event.transferable.transferDataFlavors
                    dataFlavors.forEach {
                        if (it == DataFlavor.javaFileListFlavor) {
                            val list = event.transferable.getTransferData(it) as List<*>

                            val pathList = mutableListOf<String>()
                            list.forEach { filePath ->
                                pathList.add(filePath.toString())
                            }
                            onFileDrop(pathList)
                        }
                    }
                    event.dropComplete(true)
                }
            }
            window.contentPane.add(component)
        }

        SideEffect {
            component.setBounds(
                dropBoundsBean.value.x,
                dropBoundsBean.value.y,
                dropBoundsBean.value.width,
                dropBoundsBean.value.height
            )
        }

        DisposableEffect(true) {
            onDispose {
                window.contentPane.remove(component)
            }
        }
    }
}