package io.github.aryapreetam.cmpwebview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView as AndroidWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kevinnzou.web.LoadingState
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import com.kevinnzou.web.rememberWebViewStateWithHTMLData
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  val messageBridge = remember { AndroidBridge(callbacks.onScriptResult) }
  var webViewInstance by remember { mutableStateOf<AndroidWebView?>(null) }

  // Create appropriate WebView state based on content type
  val webViewState = when (content) {
    is WebViewContent.Url -> {
      rememberWebViewState(url = content.url, additionalHttpHeaders = content.headers ?: emptyMap())
    }
    is WebViewContent.Html -> {
      rememberWebViewStateWithHTMLData(
        data = content.htmlContent,
        baseUrl = content.baseUrl,
        encoding = "UTF-8",
        mimeType = "text/html"
      )
    }
  }

  val navigator = rememberWebViewNavigator()

  // Monitor loading state and trigger callbacks
  LaunchedEffect(webViewState.loadingState, webViewInstance) {
    when (webViewState.loadingState) {
      is LoadingState.Loading -> {
        callbacks.onLoadStarted?.invoke()
      }
      is LoadingState.Finished -> {
        // Inject bridge script once per successful load (deterministic, no polling)
        webViewInstance?.evaluateJavascript(BRIDGE_SCRIPT, null)
        callbacks.onLoadFinished?.invoke()
      }
      is LoadingState.Initializing -> {
      }
    }
  }

  WebView(
    modifier = modifier,
    state = webViewState,
    navigator = navigator,
    onCreated = { webView ->
      webViewInstance = webView

      // Fix black flicker by setting transparent background
      webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

      @SuppressLint("SetJavaScriptEnabled") // Required for bridge functionality
      webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
      }

      // Add JavaScript bridge interface
      @SuppressLint("JavascriptInterface") // AndroidBridge class has @JavascriptInterface annotation
      webView.addJavascriptInterface(messageBridge, "AndroidBridge")

      // Inject bridge script immediately for initial content
      webView.evaluateJavascript(BRIDGE_SCRIPT, null)
    },
    onDispose = { webView ->
      webView.removeJavascriptInterface("AndroidBridge")
      webViewInstance = null
    }
  )
}

private class AndroidBridge(private val onScriptResult: ((String) -> Unit)?) {
  @JavascriptInterface
  @Suppress("unused") // Called from JavaScript
  fun postMessage(message: String) {
    try {
      onScriptResult?.invoke(message)
    } catch (e: Exception) {
      // Best-effort: avoid throwing across JS boundary
    }
  }
}
