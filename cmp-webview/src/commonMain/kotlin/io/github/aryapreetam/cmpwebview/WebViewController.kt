package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlin.concurrent.Volatile

@Stable
interface WebViewController {
  suspend fun evaluateJavaScript(script: String): WebViewJsResult
  fun reload()
  fun goBack(): Boolean
  fun goForward(): Boolean
}

sealed interface WebViewJsResult {
  data class Success(val rawJsonOrString: String?) : WebViewJsResult
  data class Unsupported(val reason: String) : WebViewJsResult
  data class Error(val message: String, val cause: Throwable? = null) : WebViewJsResult
}

@Composable
fun rememberWebViewController(): WebViewController {
  return remember { WebViewControllerImpl() }
}

@Stable
internal class WebViewControllerImpl : WebViewController {
  @Volatile
  private var bindings: Bindings? = null

  internal fun attach(bindings: Bindings) {
    this.bindings = bindings
  }

  internal fun detach() {
    bindings = null
  }

  override suspend fun evaluateJavaScript(script: String): WebViewJsResult {
    val bound = bindings
      ?: return WebViewJsResult.Unsupported("No WebView instance is attached to this controller yet")
    return bound.evaluateJavaScript(script)
  }

  override fun reload() {
    bindings?.reload?.invoke()
  }

  override fun goBack(): Boolean {
    return bindings?.goBack?.invoke() ?: false
  }

  override fun goForward(): Boolean {
    return bindings?.goForward?.invoke() ?: false
  }

  internal class Bindings(
    val evaluateJavaScript: suspend (String) -> WebViewJsResult,
    val reload: () -> Unit,
    val goBack: () -> Boolean,
    val goForward: () -> Boolean
  )
}
