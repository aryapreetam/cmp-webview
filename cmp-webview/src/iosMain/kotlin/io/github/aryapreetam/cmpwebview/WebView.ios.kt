package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import io.github.aryapreetam.cmpwebview.internal.bridge.resolveBridgeEnablement
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.WebKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
) {
  val messageHandler = remember { IOSMessageHandler() }
  val navigationDelegate = remember { IOSNavigationDelegate() }

  // Ensure handler/delegate always see the latest callbacks across recompositions
  SideEffect {
    messageHandler.onScriptResult = callbacks.onScriptResult
    navigationDelegate.callbacks = callbacks
  }

  val bridgeEnablement = remember(options, callbacks.onScriptResult, controller) {
    resolveBridgeEnablement(options, callbacks, controller)
  }
  val jsToComposeEnabled = bridgeEnablement.jsToCompose

  val webView = remember {
    val config = WKWebViewConfiguration()
    WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
      setNavigationDelegate(navigationDelegate)
    }
  }

  // Install/remove bridge only when actually used.
  DisposableEffect(jsToComposeEnabled) {
    val ucc = webView.configuration.userContentController

    if (jsToComposeEnabled) {
      val userScript = WKUserScript(
        source = BRIDGE_SCRIPT,
        injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
        forMainFrameOnly = false
      )

      // Ensure idempotent state.
      ucc.removeAllUserScripts()
      ucc.removeScriptMessageHandlerForName("iosBridge")

      ucc.addUserScript(userScript)
      ucc.addScriptMessageHandler(messageHandler, "iosBridge")

      // Best-effort: inject into current document too.
      webView.evaluateJavaScript(BRIDGE_SCRIPT, null)
    } else {
      ucc.removeAllUserScripts()
      ucc.removeScriptMessageHandlerForName("iosBridge")
    }

    onDispose {
      // No-op; state transitions handled above.
    }
  }

  DisposableEffect(controller) {
    val impl = controller as? WebViewControllerImpl
    if (impl != null) {
      impl.attach(
        WebViewControllerImpl.Bindings(
          evaluateJavaScript = { script ->
            suspendCancellableCoroutine { continuation ->
              webView.evaluateJavaScript(script) { result, error ->
                if (!continuation.isActive) return@evaluateJavaScript

                when {
                  error != null -> {
                    continuation.resume(WebViewJsResult.Error(error.localizedDescription, null))
                  }
                  result == null -> {
                    continuation.resume(WebViewJsResult.Success(null))
                  }
                  result is String -> {
                    continuation.resume(WebViewJsResult.Success(result))
                  }
                  else -> {
                    continuation.resume(WebViewJsResult.Success(result.toString()))
                  }
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

  // Load content whenever it changes, but keep the bridge handler installed for the lifetime
  // of the WKWebView (don’t remove it on every content update).
  LaunchedEffect(content) {
    webView.stopLoading()

    when (content) {
      is WebViewContent.Url -> {
        val nsUrl = NSURL.URLWithString(content.url)
        val request = NSURLRequest.requestWithURL(nsUrl!!)

        // Note: WKWebView doesn't support custom headers in loadRequest.
        // For headers support, we'd need to use URLSession + custom loading.
        webView.loadRequest(request)
      }

      is WebViewContent.Html -> {
        val baseUrl = content.baseUrl?.let { NSURL.URLWithString(it) }
        webView.loadHTMLString(
          string = content.htmlContent,
          baseURL = baseUrl
        )
      }
    }
  }

  // Cleanup only when the composable leaves composition.
  DisposableEffect(Unit) {
    onDispose {
      webView.configuration.userContentController.removeScriptMessageHandlerForName("iosBridge")
      webView.configuration.userContentController.removeAllUserScripts()
      webView.stopLoading()
    }
  }

  UIKitView(
    factory = { webView },
    modifier = modifier
  )
}

@OptIn(ExperimentalForeignApi::class)
private class IOSMessageHandler(
) : NSObject(), WKScriptMessageHandlerProtocol {
  var onScriptResult: ((String) -> Unit)? = null

  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as? String
    message?.let { onScriptResult?.invoke(unwrapBridgeMessage(it)) }
  }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSNavigationDelegate(
) : NSObject(), WKNavigationDelegateProtocol {
  var callbacks: WebViewCallbacks = WebViewCallbacks.EMPTY

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
    callbacks.onLoadStarted?.invoke()
  }

  @ObjCSignatureOverride
  override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
    callbacks.onLoadFinished?.invoke()
  }

  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView,
    didFailProvisionalNavigation: WKNavigation?,
    withError: platform.Foundation.NSError
  ) {
    callbacks.onLoadError?.invoke(withError.localizedDescription)
  }

  @ObjCSignatureOverride
  override fun webView(
    webView: WKWebView,
    didFailNavigation: WKNavigation?,
    withError: platform.Foundation.NSError
  ) {
    callbacks.onLoadError?.invoke(withError.localizedDescription)
  }
}
