import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.wpf.util.common.MainView
import com.wpf.util.common.ui.utils.WindowDraggableArea

fun main() = application {
    Window(
        transparent = true,
        undecorated = true,
        resizable = false, state = rememberWindowState(
            position = WindowPosition(
                Alignment.Center
            )
        ),
        title = "打渠道包并签名", onCloseRequest = ::exitApplication
    ) {
        MainView(this, this@application)
    }
}
