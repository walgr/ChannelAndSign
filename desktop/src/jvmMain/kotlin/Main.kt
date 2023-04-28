import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.wpf.util.common.MainView

fun main() = application {
    Window(
        resizable = false, state = rememberWindowState(position = WindowPosition(
            Alignment.Center
        )),
        title = "打渠道包并签名", onCloseRequest = ::exitApplication
    ) {
        MainView(window)
    }
}
