package io.github.aryapreetam.cmpwebview.internal.models

/**
 * Container for all WebView lifecycle and bridge callbacks.
 * Internal use only - simplifies parameter passing to platform implementations.
 */
internal data class WebViewCallbacks(
  /**
   * Callback for JavaScript bridge messages.
   * Receives string message sent from web content via ComposeWebViewBridge.
   */
  val onScriptResult: ((String) -> Unit)?,

  /**
   * Callback when page/content load begins.
   */
  val onLoadStarted: (() -> Unit)?,

  /**
   * Callback when page/content load completes successfully.
   */
  val onLoadFinished: (() -> Unit)?,

  /**
   * Callback when page/content load fails.
   * Receives error message describing the failure.
   */
  val onLoadError: ((String) -> Unit)?
) {
  companion object {
    /**
     * Empty callbacks (no-op) for when user doesn't provide any.
     */
    val EMPTY = WebViewCallbacks(
      onScriptResult = null,
      onLoadStarted = null,
      onLoadFinished = null,
      onLoadError = null
    )
  }
}

