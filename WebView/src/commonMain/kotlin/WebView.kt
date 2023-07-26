import LoadingState.Finished
import LoadingState.Loading
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import javafx.scene.web.WebView
import java.net.URI
import kotlinx.coroutines.CoroutineScope

@Composable
fun WebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
    navigator: WebViewNavigator,
    onCreated: WebView.() -> Unit = {},
    onDispose: WebView.() -> Unit = {},
) {
    WebViewImpl(state, modifier, navigator, onCreated, onDispose)
}

@Composable
internal expect fun WebViewImpl(
    state: WebViewState,
    modifier: Modifier = Modifier,
    navigator: WebViewNavigator,
    onCreated: WebView.() -> Unit = {},
    onDispose: WebView.() -> Unit = {},
)


expect object CookieManagerCompat {

    fun setCookie(url: String, headers: MutableMap<String, String>)

    fun setCookie(url: String, cookies: List<String>)

    fun getCookie(uri: URI): MutableMap<String, String>?
}


expect class WebViewError

/**
 * A state holder to hold the state for the WebView. In most cases this will be remembered
 * using the rememberWebViewState(uri) function.
 */
@Stable
expect class WebViewState(webContent: WebContent)

expect sealed class WebContent

expect class WebViewNavigator(coroutineScope: CoroutineScope)

/**
 * Sealed class for constraining possible loading states.
 * See [Loading] and [Finished].
 */
sealed class LoadingState {
    /**
     * Describes a WebView that has not yet loaded for the first time.
     */
    object Initializing : LoadingState()

    /**
     * Describes a webview between `onPageStarted` and `onPageFinished` events, contains a
     * [progress] property which is updated by the webview.
     */
    data class Loading(val progress: Float) : LoadingState()

    /**
     * Describes a webview that has finished loading content.
     */
    object Finished : LoadingState()
}

typealias UrlChange = (String?, WebView) -> Unit

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param url The url to load in the WebView
 * @param additionalHttpHeaders Optional, additional HTTP headers that are passed to [WebViewDesktop.loadUrl].
 *                              Note that these headers are used for all subsequent requests of the WebView.
 */
@Composable
expect fun rememberWebViewState(url: String, additionalHttpHeaders: Map<String, String> = emptyMap(), urlChange: UrlChange? = null): WebViewState

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param data The uri to load in the WebView
 */
@Composable
expect fun rememberWebViewStateWithHTMLData(data: String, baseUrl: String? = null): WebViewState


/**
 * WebViewNavigator control WebView
 * - fun navigateBack()
 * - fun navigateForward()
 * - fun reload()
 * - fun stopLoading()
 */
@Composable
fun rememberWebViewNavigator(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): WebViewNavigator = remember(coroutineScope) { WebViewNavigator(coroutineScope) }

