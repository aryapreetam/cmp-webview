package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
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
  val messageHandler = remember { IOSMessageHandler(callbacks.onScriptResult) }
  val navigationDelegate = remember { IOSNavigationDelegate(callbacks) }

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

  DisposableEffect(content) {
    when (content) {
      is WebViewContent.Url -> {
        val nsUrl = NSURL.URLWithString(content.url)
        val request = NSURLRequest.requestWithURL(nsUrl!!)

        // Note: WKWebView doesn't support custom headers in loadRequest
        // For headers support, would need to use URLSession
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
  private val onScriptResult: ((String) -> Unit)?
) : NSObject(), WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as? String
    message?.let { onScriptResult?.invoke(it) }
  }
}

@OptIn(ExperimentalForeignApi::class)
private class IOSNavigationDelegate(
  private val callbacks: WebViewCallbacks
) : NSObject(), WKNavigationDelegateProtocol {
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
