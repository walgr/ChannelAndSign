package com.wpf.util.common.ui.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.WindowScope
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

/**
 * WindowDraggableArea is a component that allows you to drag the window using the mouse.
 *
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
fun WindowScope.WindowDraggableArea(
    modifier: Modifier = Modifier, content: @Composable () -> Unit = {}
) {
    val handler = remember { DragHandler(window) }

    Box(modifier = modifier.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown()
            handler.register()
        }
    }) {
        content()
    }
}

private class DragHandler(private val window: Window) {
    private var location = window.location.toComposeOffset()
    private var pointStart = MouseInfo.getPointerInfo().location.toComposeOffset()

    private val dragListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) = drag()
    }
    private val removeListener = object : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) {
            window.removeMouseMotionListener(dragListener)
            window.removeMouseListener(this)
        }
    }

    fun register() {
        location = window.location.toComposeOffset()
        pointStart = MouseInfo.getPointerInfo().location.toComposeOffset()
        window.addMouseListener(removeListener)
        window.addMouseMotionListener(dragListener)
    }

    private fun drag() {
        val point = MouseInfo.getPointerInfo().location.toComposeOffset()
        val location = location + (point - pointStart)
        window.setLocation(location.x, location.y)
    }

    private fun Point.toComposeOffset() = IntOffset(x, y)
}