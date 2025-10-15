package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class, kotlin.js.ExperimentalWasmJsInterop::class)
@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  val iframe = remember {
    (document.createElement("iframe") as HTMLIFrameElement).apply {
      style.width = "100%"
      style.height = "100%"
      style.border = "none"
    }
  }

  DisposableEffect(content) {
    // Message listener for bridge communication
    val messageHandler: (Event) -> Unit = { event ->
      if (event is MessageEvent) {
        @OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
        callbacks.onScriptResult?.invoke(event.data.toString())
      }
    }
    window.addEventListener("message", messageHandler)

    // Load started callback
    val loadStartHandler: (Event) -> Unit = {
      callbacks.onLoadStarted?.invoke()
    }
    iframe.addEventListener("loadstart", loadStartHandler)

    // Load finished callback
    val loadHandler: (Event) -> Unit = {
      callbacks.onLoadFinished?.invoke()

      // Note: Bridge script injection in WASM only works for HTML content (srcdoc)
      // For external URLs, cross-origin restrictions prevent script injection
    }
    iframe.addEventListener("load", loadHandler)

    // Error handler
    val errorHandler: (Event) -> Unit = {
      callbacks.onLoadError?.invoke("Failed to load content")
    }
    iframe.addEventListener("error", errorHandler)

    // Load content
    when (content) {
      is WebViewContent.Url -> {
        iframe.src = content.url
      }

      is WebViewContent.Html -> {
        // Inject bridge script into HTML content
        val htmlWithBridge = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <script>
                        $BRIDGE_SCRIPT
                        </script>
                    </head>
                    <body>
                    ${content.htmlContent}
                    </body>
                    </html>
                """.trimIndent()
        iframe.srcdoc = htmlWithBridge
      }
    }

    onDispose {
      window.removeEventListener("message", messageHandler)
      iframe.removeEventListener("loadstart", loadStartHandler)
      iframe.removeEventListener("load", loadHandler)
      iframe.removeEventListener("error", errorHandler)
      iframe.remove()
    }
  }

  WebElementView(
    factory = { iframe },
    modifier = modifier,
    update = { /* content changes handled by DisposableEffect */ }
  )
}