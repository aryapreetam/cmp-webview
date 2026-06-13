package io.github.aryapreetam.cmpwebview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView as AndroidWebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import io.github.aryapreetam.cmpwebview.internal.bridge.resolveBridgeEnablement
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
) {
  val messageBridge = remember { AndroidBridge() }
  var webViewInstance by remember { mutableStateOf<AndroidWebView?>(null) }

  val bridgeEnablement = remember(options, callbacks.onScriptResult, controller) {
    resolveBridgeEnablement(options, callbacks, controller)
  }
  val jsToComposeEnabled = bridgeEnablement.jsToCompose

  DisposableEffect(controller, webViewInstance) {
    val impl = controller as? WebViewControllerImpl
    val webView = webViewInstance

    if (impl != null && webView != null) {
      impl.attach(
        WebViewControllerImpl.Bindings(
          evaluateJavaScript = { script ->
            suspendCancellableCoroutine { continuation ->
              try {
                // Ensure call happens on the WebView thread.
                webView.post {
                  try {
                    webView.evaluateJavascript(script) { rawResult ->
                      if (continuation.isActive) {
                        continuation.resume(WebViewJsResult.Success(rawResult))
                      }
                    }
                  } catch (t: Throwable) {
                    if (continuation.isActive) {
                      continuation.resume(WebViewJsResult.Error("evaluateJavaScript failed", t))
                    }
                  }
                }
              } catch (t: Throwable) {
                if (continuation.isActive) {
                  continuation.resume(WebViewJsResult.Error("evaluateJavaScript failed", t))
                }
              }
            }
          },
          reload = { webView.reload() },
          goBack = {
            if (webView.canGoBack()) {
              webView.goBack()
              true
            } else {
              false
            }
          },
          goForward = {
            if (webView.canGoForward()) {
              webView.goForward()
              true
            } else {
              false
            }
          }
        )
      )
    }

    onDispose {
      impl?.detach()
    }
  }

  SideEffect {
    messageBridge.onScriptResult = callbacks.onScriptResult
  }

  val bridgeEnabled = jsToComposeEnabled

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
  LaunchedEffect(webViewState.loadingState, webViewInstance, bridgeEnabled) {
    when (webViewState.loadingState) {
      is LoadingState.Loading -> {
        callbacks.onLoadStarted?.invoke()
      }
      is LoadingState.Finished -> {
        // Inject bridge script once per successful load (deterministic, no polling)
        if (bridgeEnabled) {
          webViewInstance?.evaluateJavascript(BRIDGE_SCRIPT, null)
        }
        callbacks.onLoadFinished?.invoke()
      }
      is LoadingState.Initializing -> {
      }
    }
  }

  // Ensure the JS interface is only installed when the bridge is actually used.
  LaunchedEffect(bridgeEnabled, webViewInstance) {
    val webView = webViewInstance ?: return@LaunchedEffect
    if (bridgeEnabled) {
      @SuppressLint("JavascriptInterface") // AndroidBridge has @JavascriptInterface
      webView.addJavascriptInterface(messageBridge, "AndroidBridge")
      webView.evaluateJavascript(BRIDGE_SCRIPT, null)
    } else {
      webView.removeJavascriptInterface("AndroidBridge")
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
    },
    onDispose = { webView ->
      webView.removeJavascriptInterface("AndroidBridge")
      webViewInstance = null
    }
  )
}

private class AndroidBridge {
  var onScriptResult: ((String) -> Unit)? = null

  @JavascriptInterface
  @Suppress("unused") // Called from JavaScript
  fun postMessage(message: String) {
    try {
      onScriptResult?.invoke(unwrapBridgeMessage(message))
    } catch (e: Exception) {
      // Best-effort: avoid throwing across JS boundary
    }
  }
}
