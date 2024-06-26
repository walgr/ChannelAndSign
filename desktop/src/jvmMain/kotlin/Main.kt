import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.wpf.util.common.MainView

fun main() = application {
    Window(
        transparent = true,
        undecorated = true,
        resizable = true, state = rememberWindowState(
            position = WindowPosition(
                Alignment.Center
            ),
            width = 1080.dp,
            height = 720.dp,
        ),
        title = "打渠道包并签名", onCloseRequest = ::exitApplication
    ) {
        MainView(this, this@application)
    }
}
