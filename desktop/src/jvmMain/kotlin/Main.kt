import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.wpf.util.common.MainView

fun main() = application {
    Window(title = "打渠道包并签名", onCloseRequest = ::exitApplication) {
        MainView(window)
    }
}
