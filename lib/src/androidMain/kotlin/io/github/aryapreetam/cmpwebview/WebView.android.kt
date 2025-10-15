package io.github.aryapreetam.cmpwebview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  val context = LocalContext.current

  val webView = remember {
    WebView(context).apply {
      @Suppress("SetJavaScriptEnabled") // Required for bridge functionality
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true

      // Add JavaScript bridge interface
      addJavascriptInterface(
        AndroidBridge(callbacks.onScriptResult),
        "AndroidBridge"
      )

      webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
          super.onPageStarted(view, url, favicon)
          // Inject bridge script as soon as page starts loading
          view?.evaluateJavascript(BRIDGE_SCRIPT, null)
          callbacks.onLoadStarted?.invoke()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
          super.onPageFinished(view, url)
          callbacks.onLoadFinished?.invoke()
        }

        @Deprecated("Use onReceivedError with WebResourceRequest", ReplaceWith(""))
        override fun onReceivedError(
          view: WebView?,
          errorCode: Int,
          description: String?,
          failingUrl: String?
        ) {
          @Suppress("DEPRECATION")
          super.onReceivedError(view, errorCode, description, failingUrl)
          callbacks.onLoadError?.invoke(description ?: "Unknown error")
        }
      }
    }
  }

  DisposableEffect(content) {
    when (content) {
      is WebViewContent.Url -> {
        // Validate URL scheme for security
        val url = content.url.lowercase()
        if (url.startsWith("javascript:") ||
          url.startsWith("vbscript:") ||
          url.startsWith("file:")
        ) {
          callbacks.onLoadError?.invoke("Blocked dangerous URL scheme: $url")
        } else {
          if (content.headers != null) {
            webView.loadUrl(content.url, content.headers)
          } else {
            webView.loadUrl(content.url)
          }
        }
      }

      is WebViewContent.Html -> {
        webView.loadDataWithBaseURL(
          content.baseUrl ?: "about:blank",
          content.htmlContent,
          "text/html",
          "UTF-8",
          null
        )
      }
    }

    onDispose {
      webView.removeJavascriptInterface("AndroidBridge")
      webView.destroy()
    }
  }

  AndroidView(
    factory = { webView },
    modifier = modifier
  )
}

private class AndroidBridge(private val onScriptResult: ((String) -> Unit)?) {
  @JavascriptInterface
  @Suppress("unused") // Called from JavaScript
  fun postMessage(message: String) {
    try {
      onScriptResult?.invoke(message)
    } catch (e: Exception) {
      println("Android bridge error: ${e.message}")
    }
  }
}
