package io.github.aryapreetam.cmpwebview

import android.webkit.JavascriptInterface
import android.webkit.WebView as AndroidWebView
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CmpWebView"

@OptIn(DelicateCoroutinesApi::class)
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
  LaunchedEffect(webViewState.loadingState) {
    when (webViewState.loadingState) {
      is LoadingState.Loading -> {
        Log.d(TAG, "Loading state: Loading")
        callbacks.onLoadStarted?.invoke()
      }
      is LoadingState.Finished -> {
        Log.d(TAG, "Loading state: Finished")
        callbacks.onLoadFinished?.invoke()
      }
      is LoadingState.Initializing -> {
        Log.d(TAG, "Loading state: Initializing")
      }
    }
  }

  // Effect to repeatedly inject bridge script until page is fully loaded
  DisposableEffect(content, webViewInstance) {
    Log.d(TAG, "DisposableEffect triggered for content change")

    val job = GlobalScope.launch(Dispatchers.Main) {
      // Inject multiple times with delays to ensure it's available
      repeat(10) { attempt ->
        delay(100L * attempt) // 0ms, 100ms, 200ms, ... 900ms
        webViewInstance?.let { webView ->
          Log.d(TAG, "Injecting bridge script (attempt ${attempt + 1})")
          webView.evaluateJavascript(BRIDGE_SCRIPT) { result ->
            Log.d(TAG, "Bridge injection attempt ${attempt + 1} result: $result")
          }
        }
      }
    }

    onDispose {
      Log.d(TAG, "DisposableEffect disposed")
      job.cancel()
    }
  }

  WebView(
    modifier = modifier,
    state = webViewState,
    navigator = navigator,
    onCreated = { webView ->
      Log.d(TAG, "WebView created, setting up bridge")
      webViewInstance = webView

      // Fix black flicker by setting transparent background
      webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

      webView.settings.apply {
        @Suppress("SetJavaScriptEnabled") // Required for bridge functionality
        javaScriptEnabled = true
        domStorageEnabled = true
        Log.d(TAG, "JavaScript enabled: $javaScriptEnabled")
      }

      // Add JavaScript bridge interface
      @Suppress("JavascriptInterface") // AndroidBridge class has @JavascriptInterface annotation
      webView.addJavascriptInterface(messageBridge, "AndroidBridge")
      Log.d(TAG, "AndroidBridge interface added")

      // Verify the interface was added
      webView.evaluateJavascript("typeof window.AndroidBridge") { result ->
        Log.d(TAG, "AndroidBridge type check: $result")
      }

      // Inject bridge script immediately
      webView.evaluateJavascript(BRIDGE_SCRIPT) { result ->
        Log.d(TAG, "Bridge script injected on onCreate: result=$result")
      }
    },
    onDispose = { webView ->
      Log.d(TAG, "WebView disposing, cleaning up bridge")
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
      Log.d(TAG, "✅ Bridge received message: $message")
      onScriptResult?.invoke(message)
    } catch (e: Exception) {
      Log.e(TAG, "❌ Android bridge error: ${e.message}", e)
      println("Android bridge error: ${e.message}")
    }
  }
}
