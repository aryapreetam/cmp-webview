package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
@androidx.compose.runtime.Composable
actual fun WebViewImpl(url: String, onScriptResult: ((String) -> Unit)?) {
  DisposableEffect(Unit) {
    // Setup message listener for iframe communication
    val messageHandler: (Event) -> Unit = { event ->
      if (event is MessageEvent) {
        onScriptResult?.invoke(event.data.toString())
      }
    }

    window.addEventListener("message", messageHandler)

    onDispose {
      window.removeEventListener("message", messageHandler)
    }
  }

  WebElementView(
    factory = {
      (document.createElement("iframe")
        as HTMLIFrameElement)
        .apply {
          id = "map-iframe"
          srcdoc = url
          style.width = "100vw"
          style.height = "100vh"
          style.zIndex = "9999"
          style.background = "transparent"
        }
    },
    modifier = Modifier.fillMaxSize(),
    update = { iframe -> iframe.srcdoc = iframe.srcdoc }
  )
}