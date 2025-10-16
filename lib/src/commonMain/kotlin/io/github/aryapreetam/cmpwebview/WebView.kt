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
 * Loads web content from a remote URL with optional custom headers. Supports JavaScript bridge
 * communication via the ComposeWebViewBridge JavaScript API.
 *
 * ## Security Considerations
 * - Only HTTPS URLs are recommended for production use
 * - URLs with dangerous schemes (javascript:, vbscript:, file:) are rejected
 * - Validate and sanitize URLs from user input before loading
 * - Use custom headers cautiously to avoid exposing sensitive data
 *
 * ## Testing
 * Use `Modifier.testTag()` to identify the WebView in UI tests:
 * ```kotlin
 * WebView(
 *     url = "https://example.com",
 *     modifier = Modifier.testTag("my-webview")
 * )
 * composeTestRule.onNodeWithTag("my-webview").assertExists()
 * ```
 *
 * ## Accessibility
 * The WebView automatically provides semantic information for screen readers,
 * including content description and loading state.
 *
 * ## Example Usage
 * ```kotlin
 * WebView(
 *     url = "https://example.com",
 *     modifier = Modifier.testTag("example-webview"),
 *     headers = mapOf("Authorization" to "Bearer token"),
 *     onLoadStarted = { println("Loading started") },
 *     onLoadFinished = { println("Loading finished") },
 *     onLoadError = { error -> println("Error: $error") },
 *     onScriptResult = { message -> println("JS message: $message") }
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
  onLoadError: ((String) -> Unit)? = null
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

  WebViewImpl(content, callbacks, modifierWithSemantics)
}

/**
 * WebView composable for loading HTML content.
 *
 * Displays HTML content directly without loading from a remote server. Useful for
 * offline content, dynamically generated HTML, or embedded content.
 *
 * ## Security Considerations
 * - Sanitize HTML content from untrusted sources to prevent XSS attacks
 * - Be cautious with baseUrl - it affects relative link resolution
 * - JavaScript in HTML content can communicate via ComposeWebViewBridge
 *
 * ## Testing
 * Use `Modifier.testTag()` to identify the WebView in UI tests:
 * ```kotlin
 * WebView(
 *     htmlContent = "<html><body><h1>Hello</h1></body></html>",
 *     modifier = Modifier.testTag("html-webview")
 * )
 * composeTestRule.onNodeWithTag("html-webview").assertExists()
 * ```
 *
 * ## Accessibility
 * The WebView automatically provides semantic information for screen readers,
 * including content description and loading state.
 *
 * ## Example Usage
 * ```kotlin
 * WebView(
 *     htmlContent = "<html><body><h1>Hello World</h1></body></html>",
 *     modifier = Modifier.testTag("html-webview"),
 *     baseUrl = "https://example.com",
 *     onLoadStarted = { println("Loading started") },
 *     onLoadFinished = { println("Loading finished") },
 *     onScriptResult = { message -> println("JS message: $message") }
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
  onLoadError: ((String) -> Unit)? = null
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

  WebViewImpl(content, callbacks, modifierWithSemantics)
}

/**
 * Platform-specific WebView implementation.
 * Each platform provides its own actual implementation.
 */
@Composable
internal expect fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
)
