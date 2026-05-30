package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.bridge.resolveBridgeEnablement
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import io.github.kdroidfilter.webview.web.*
import io.github.kdroidfilter.webview.jsbridge.*
import kotlinx.coroutines.flow.collectLatest

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
) {
  // Delegate state creation to the library's own composable factory functions.
  // These create the WebViewState ONCE and reactively update state.content on
  // every recomposition when the URL/data parameters change. The library's
  // internal WebView composable observes state.content via Compose snapshot
  // state and calls loadContent() on the native DesktopWebView (Wry engine),
  // mirroring how the Android implementation uses rememberWebViewState(url).
  val state = when (content) {
    is WebViewContent.Url -> {
      rememberWebViewState(
        url = content.url,
        additionalHttpHeaders = content.headers ?: emptyMap()
      )
    }
    is WebViewContent.Html -> {
      rememberWebViewStateWithHTMLData(
        data = content.htmlContent,
        baseUrl = content.baseUrl,
        encoding = "utf-8",
        mimeType = "text/html"
      )
    }
  }

  val navigator = rememberWebViewNavigator()
  val jsBridge = rememberWebViewJsBridge()

  // Bind the WebViewController commands to the native WebViewNavigator.
  DisposableEffect(controller, navigator) {
    val impl = controller as? WebViewControllerImpl
    if (impl != null) {
      impl.attach(
        WebViewControllerImpl.Bindings(
          evaluateJavaScript = { script ->
            try {
              val result = navigator.evaluateJavaScript(script)
              WebViewJsResult.Success(result.toString())
            } catch (t: Throwable) {
              WebViewJsResult.Error("evaluateJavaScript failed", t)
            }
          },
          reload = { navigator.reload() },
          goBack = {
            if (navigator.canGoBack) {
              navigator.navigateBack()
              true
            } else {
              false
            }
          },
          goForward = {
            if (navigator.canGoForward) {
              navigator.navigateForward()
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

  // Track page loading state changes reactively and trigger appropriate callbacks.
  val loadingState = state.loadingState
  LaunchedEffect(loadingState) {
    when (loadingState) {
      is LoadingState.Initializing -> {
        // No-op for initializing phase
      }
      is LoadingState.Loading -> {
        callbacks.onLoadStarted?.invoke()
      }
      is LoadingState.Finished -> {
        // Transparent JS Bridge injection polyfill.
        // We inject window.javaBridge.postMessage which maps directly to Wry's window.kmpJsBridge.callNative.
        // This ensures existing web applications do not need any code modification.
        val bridgeSetupScript = """
          (function() {
            if (!window.javaBridge) {
              window.javaBridge = {
                postMessage: function(envelope) {
                  if (window.kmpJsBridge && typeof window.kmpJsBridge.callNative === 'function') {
                    window.kmpJsBridge.callNative("postMessage", JSON.stringify(envelope), function() {});
                  }
                }
              };
            }
          })();
        """.trimIndent()

        try {
          navigator.evaluateJavaScript(bridgeSetupScript)
          navigator.evaluateJavaScript(BRIDGE_SCRIPT)
        } catch (t: Throwable) {
          if (System.getProperty("cmpwebview.desktop.debug") == "true") {
            println("[cmp-webview][desktop] Failed to inject JS bridge scripts: ${t.message}")
          }
        }
        callbacks.onLoadFinished?.invoke()
      }
    }
  }

  // Observe and propagate loading errors on the main frame.
  LaunchedEffect(state) {
    snapshotFlow { state.errorsForCurrentRequest.toList() }
      .collectLatest { currentErrors ->
        val mainFrameError = currentErrors.firstOrNull { it.isFromMainFrame }
        if (mainFrameError != null) {
          callbacks.onLoadError?.invoke("${mainFrameError.description} (code: ${mainFrameError.code})")
        }
      }
  }

  // Configure bridge enablement parameters dynamically.
  val bridgeEnablement = remember(options, callbacks.onScriptResult, controller) {
    resolveBridgeEnablement(options, callbacks, controller)
  }

  // Register JS bridge interface to capture incoming postMessage envelopes.
  LaunchedEffect(jsBridge, bridgeEnablement, callbacks.onScriptResult) {
    if (bridgeEnablement.jsToCompose && callbacks.onScriptResult != null) {
      jsBridge.register(object : IJsMessageHandler {
        override fun methodName(): String = "postMessage"

        override fun handle(
          message: JsMessage,
          navigator: WebViewNavigator?,
          callback: (String) -> Unit
        ) {
          var rawParams = message.params
          if (rawParams.startsWith("\"") && rawParams.endsWith("\"")) {
            rawParams = rawParams.substring(1, rawParams.length - 1)
          }
          rawParams = rawParams.replace("\\\"", "\"").replace("\\\\", "\\")

          val payload = unwrapBridgeMessage(rawParams)
          callbacks.onScriptResult.invoke(payload)
          callback("")
        }
      })
    }
  }

  // Render the native WebView composable from ComposeNativeWebview library.
  io.github.kdroidfilter.webview.web.WebView(
    state = state,
    navigator = navigator,
    webViewJsBridge = jsBridge,
    modifier = modifier.fillMaxSize()
  )
}
