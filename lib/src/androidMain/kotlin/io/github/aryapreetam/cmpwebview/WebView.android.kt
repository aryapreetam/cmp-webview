package io.github.aryapreetam.cmpwebview

import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewStateWithHTMLData

@androidx.compose.runtime.Composable
actual fun WebViewImpl(url: String, onScriptResult: ((String) -> Unit)?) {
  val webViewState = rememberWebViewStateWithHTMLData(data = url, encoding = "UTF-8", mimeType = "text/html")
  val messageBridge = remember { MessageBridge(onScriptResult) }

  WebView(
    modifier = Modifier.fillMaxSize(),
    state = webViewState,
    onCreated = { webView ->
      webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
      }
      // Add JavaScript interface
      webView.addJavascriptInterface(messageBridge, "AndroidMessageBridge")
    }
  )
}

private class MessageBridge(private val onScriptResult: ((String) -> Unit)?) {
  @JavascriptInterface
  fun onMessageUpdate(data: String) {
    try {
      onScriptResult?.invoke(data)
    } catch (e: Exception) {
      println( "Error in the bridge: ${e.message}" )
    }
  }
}
