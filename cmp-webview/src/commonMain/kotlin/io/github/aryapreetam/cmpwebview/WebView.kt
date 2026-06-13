package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent

/**
 * WebView composable for loading remote URLs.
 *
 * Loads web content from a remote URL with optional custom headers. Supports JavaScript-to-Compose
 * communication via the `ComposeWebViewBridge.postMessage()` JavaScript API.
 *
 * **Note:** Android requires `INTERNET` permission in AndroidManifest.xml to load remote content.
 *
 * ## Example
 * ```kotlin
 * WebView(
 *   url = "https://example.com",
 *   modifier = Modifier.fillMaxSize(),
 *   onLoadFinished = { println("Page loaded") },
 *   onScriptResult = { message -> println("From JS: $message") }
 * )
 * ```
 *
 * @param url The URL to load
 * @param modifier Compose modifier for layout and styling
 * @param headers Optional HTTP headers for the request
 * @param onScriptResult Callback for JavaScript bridge messages from ComposeWebViewBridge.postMessage()
 * @param onLoadStarted Callback when page load begins
 * @param onLoadFinished Callback when page load completes successfully
 * @param onLoadError Callback when page load fails with error message
 */
@Composable
fun WebView(
  url: String,
  modifier: Modifier = Modifier,
  headers: Map<String, String>? = null,
  onScriptResult: ((String) -> Unit)? = null,
  onLoadStarted: (() -> Unit)? = null,
  onLoadFinished: (() -> Unit)? = null,
  onLoadError: ((String) -> Unit)? = null,
  options: WebViewOptions = WebViewOptions(),
  controller: WebViewController? = null
) {
  var loadingState by remember { mutableStateOf("Idle") }

  val content = remember(url, headers) {
    WebViewContent.Url(url, headers)
  }

  val callbacks = remember(onScriptResult, onLoadStarted, onLoadFinished, onLoadError) {
    WebViewCallbacks(
      onScriptResult = onScriptResult,
      onLoadStarted = {
        loadingState = "Loading"
        onLoadStarted?.invoke()
      },
      onLoadFinished = {
        loadingState = "Loaded"
        onLoadFinished?.invoke()
      },
      onLoadError = { error ->
        loadingState = "Error"
        onLoadError?.invoke(error)
      }
    )
  }

  val modifierWithSemantics = modifier
    .semantics {
      contentDescription = "Web content display"
      stateDescription = loadingState
    }

  WebViewImpl(content, callbacks, modifierWithSemantics, options, controller)
}

/**
 * WebView composable for loading HTML content.
 *
 * Displays HTML content directly without loading from a remote server. Useful for offline content,
 * dynamically generated HTML, or embedded resources.
 *
 * **Tip:** Use `baseUrl` to resolve relative links and resources in your HTML content.
 *
 * ## Example
 * ```kotlin
 * WebView(
 *   htmlContent = """
 *     <html><body>
 *       <h1>Hello World</h1>
 *       <button onclick="ComposeWebViewBridge.postMessage('clicked')">
 *         Click Me
 *       </button>
 *     </body></html>
 *   """.trimIndent(),
 *   onScriptResult = { message -> println("Button: $message") }
 * )
 * ```
 *
 * @param htmlContent The HTML string to display
 * @param modifier Compose modifier for layout and styling
 * @param baseUrl Optional base URL for resolving relative links in the HTML content
 * @param onScriptResult Callback for JavaScript bridge messages from ComposeWebViewBridge.postMessage()
 * @param onLoadStarted Callback when content load begins
 * @param onLoadFinished Callback when content load completes successfully
 * @param onLoadError Callback when content load fails with error message
 */
@Composable
fun WebView(
  htmlContent: String,
  modifier: Modifier = Modifier,
  baseUrl: String? = null,
  onScriptResult: ((String) -> Unit)? = null,
  onLoadStarted: (() -> Unit)? = null,
  onLoadFinished: (() -> Unit)? = null,
  onLoadError: ((String) -> Unit)? = null,
  options: WebViewOptions = WebViewOptions(),
  controller: WebViewController? = null
) {
  var loadingState by remember { mutableStateOf("Idle") }

  val content = remember(htmlContent, baseUrl) {
    WebViewContent.Html(htmlContent, baseUrl)
  }

  val callbacks = remember(onScriptResult, onLoadStarted, onLoadFinished, onLoadError) {
    WebViewCallbacks(
      onScriptResult = onScriptResult,
      onLoadStarted = {
        loadingState = "Loading"
        onLoadStarted?.invoke()
      },
      onLoadFinished = {
        loadingState = "Loaded"
        onLoadFinished?.invoke()
      },
      onLoadError = { error ->
        loadingState = "Error"
        onLoadError?.invoke(error)
      }
    )
  }

  val modifierWithSemantics = modifier
    .semantics {
      contentDescription = "Web content display"
      stateDescription = loadingState
    }

  WebViewImpl(content, callbacks, modifierWithSemantics, options, controller)
}

/**
 * Platform-specific WebView implementation.
 * Each platform provides its own actual implementation.
 */
@Composable
internal expect fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
)
