import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import javafx.scene.web.WebView


@Composable
internal actual fun WebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    navigator: WebViewNavigator,
    onCreated: WebView.() -> Unit,
    onDispose: WebView.() -> Unit,
) {

    //The results of running in run and debug modes are different, which made me realize that WebEngine variables
    // are local variables, and the monitoring events inside are recycled by gc before they are executed.
    WebViewDesktop(state, modifier, navigator, onCreated = onCreated, onDispose = onDispose)
}

