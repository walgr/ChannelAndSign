import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.wpf.util.common.MainView

// 主函数，应用程序的入口
fun main() = application {
    // 创建一个窗口
    Window(
        transparent = true, // 窗口透明
        undecorated = true, // 窗口无边框
        resizable = true, // 窗口可调整大小
        state = rememberWindowState(
            // 记住窗口状态
            position = WindowPosition( // 设置窗口位置
                Alignment.Center // 窗口居中
            ),
            width = 1080.dp, // 窗口宽度为1080dp
            height = 720.dp, // 窗口高度为720dp
        ),
        title = "打渠道包并签名", // 窗口标题
        onCloseRequest = ::exitApplication // 关闭请求时退出应用程序
    ) {
        // 在窗口中显示主视图
        MainView(this, this@application)
    }
}
