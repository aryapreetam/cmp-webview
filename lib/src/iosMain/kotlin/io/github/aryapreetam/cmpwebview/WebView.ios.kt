package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURL
import platform.WebKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  val messageHandler = remember { IOSMessageHandler() }
  val navigationDelegate = remember { IOSNavigationDelegate() }

  // Ensure handler/delegate always see the latest callbacks across recompositions
  SideEffect {
    messageHandler.onScriptResult = callbacks.onScriptResult
    navigationDelegate.callbacks = callbacks
  }

  val webView = remember {
    val config = WKWebViewConfiguration()

    // Inject bridge script at document start
    val userScript = WKUserScript(
      source = BRIDGE_SCRIPT,
      injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
      forMainFrameOnly = false
    )
    config.userContentController.addUserScript(userScript)
    config.userContentController.addScriptMessageHandler(messageHandler, "iosBridge")

    WKWebView(frame = CGRectZero.readValue(), configuration = config).apply {
      setNavigationDelegate(navigationDelegate)
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
