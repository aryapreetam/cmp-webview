package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSString
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIView
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@androidx.compose.runtime.Composable
actual fun WebViewImpl(url: String, onScriptResult: ((String) -> Unit)?) {
  val config = WKWebViewConfiguration().apply {
    allowsInlineMediaPlayback = true
    allowsAirPlayForMediaPlayback = true
    allowsPictureInPictureMediaPlayback = true
  }

  val messageHandler = remember { MessageHandler(onScriptResult) }

  val webView = remember {
    WKWebView(CGRectZero.readValue(), config).apply {
      // Add script message handler
      configuration.userContentController.addScriptMessageHandler(
        messageHandler,
        "iosMessageHandler"
      )
    }
  }

  // Enable java script content
  webView.configuration.defaultWebpagePreferences.allowsContentJavaScript = true

  // Inject JavaScript to intercept location updates
  val script = WKUserScript(
    source = """
      window.postMessage = function(data) {
        window.webkit.messageHandlers.iosMessageHandler.postMessage(data);
      };
    """.trimIndent(),
    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
    forMainFrameOnly = true
  )
  webView.configuration.userContentController.addUserScript(script)

  DisposableEffect(Unit) {
    onDispose {
      // Remove the message handler when the view is disposed
      webView.configuration.userContentController.removeScriptMessageHandlerForName(
        "iosMessageHandler"
      )
    }
  }

  UIKitView(
    factory = {
      val container = UIView()

      webView.translatesAutoresizingMaskIntoConstraints = false
      container.addSubview(webView)

      NSLayoutConstraint.activateConstraints(
        listOf(
          webView.topAnchor.constraintEqualToAnchor(container.topAnchor),
          webView.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor),
          webView.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor),
          webView.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor)
        )
      )

      webView.loadHTMLString(url, baseURL = null)

      container
    },
    modifier = Modifier.fillMaxSize(),
    properties = UIKitInteropProperties(
      isInteractive = true,
      isNativeAccessibilityEnabled = true
    )
  )
}

private class MessageHandler(
  private val onScriptResult: ((String) -> Unit)?
) : NSObject(), WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage
  ) {
    val message = didReceiveScriptMessage.body as? NSString
    if (message != null) {
      println("Received message in iOS bridge: $message")
      onScriptResult?.invoke(message.toString())
    }
  }
}