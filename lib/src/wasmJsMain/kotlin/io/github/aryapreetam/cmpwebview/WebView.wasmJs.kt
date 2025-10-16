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
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  // Create the iframe once and let Compose manage its presence in the DOM
  val iframe = remember {
    (document.createElement("iframe") as HTMLIFrameElement).apply {
      style.width = "100%"
      style.height = "100%"
      style.border = "none"
      // sandbox could be tuned if needed; keep default for broader compatibility
    }
  }

  // Lifecycle: attach/remove global and element listeners once
  DisposableEffect(Unit) {
    // Message listener for bridge communication
    val messageHandler: (Event) -> Unit = { event ->
      if (event is MessageEvent) {
        callbacks.onScriptResult?.invoke(event.data.toString())
      }
    }
    window.addEventListener("message", messageHandler)

    // Load finished callback
    val loadHandler: (Event) -> Unit = {
      callbacks.onLoadFinished?.invoke()
      // Note: Bridge script injection in WASM only works for HTML content (srcdoc)
      // For external URLs, cross-origin restrictions prevent script injection
    }
    iframe.addEventListener("load", loadHandler)

    // Error handler (not all failures trigger this for iframes)
    val errorHandler: (Event) -> Unit = {
      callbacks.onLoadError?.invoke("Failed to load content")
    }
    iframe.addEventListener("error", errorHandler)

    onDispose {
      window.removeEventListener("message", messageHandler)
      iframe.removeEventListener("load", loadHandler)
      iframe.removeEventListener("error", errorHandler)
      // Do NOT remove the iframe element here; Compose owns its lifecycle
    }
  }

  // React to content changes: set src/srcdoc. Do not tear down the element itself.
  DisposableEffect(content) {
    // Signal load start before navigating the iframe
    callbacks.onLoadStarted?.invoke()

    when (content) {
      is WebViewContent.Url -> {
        // Clear any previous srcdoc to ensure URL takes effect
        if (iframe.srcdoc.isNotEmpty()) {
          iframe.srcdoc = ""
        }
        iframe.src = content.url
      }
      is WebViewContent.Html -> {
        // Build HTML with optional base URL and bridge script
        val htmlWithBridge = buildHtmlForSrcDoc(content.htmlContent, content.baseUrl)
        // Clear src so srcdoc is used
        if (iframe.src.isNotEmpty()) {
          iframe.src = "about:blank"
        }
        iframe.srcdoc = htmlWithBridge
      }
    }

    onDispose { /* nothing on content swap */ }
  }

  WebElementView(
    factory = { iframe },
    modifier = modifier,
    update = { /* content changes handled by DisposableEffect(content) */ }
  )
}

// Helper to inject bridge script and base URL into provided HTML safely for srcdoc
private fun buildHtmlForSrcDoc(html: String, baseUrl: String?): String {
  val bridgeTag = """
    <script>
    $BRIDGE_SCRIPT
    </script>
  """.trimIndent()
  val baseTag = baseUrl?.let { "<base href=\"$it\">" } ?: ""

  val trimmed = html.trimStart()
  val hasHtmlTag = trimmed.startsWith("<html", ignoreCase = true) || trimmed.startsWith("<!DOCTYPE", ignoreCase = true)
  val hasHeadTag = "<head".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(html)
  val hasBodyTag = "<body".toRegex(RegexOption.IGNORE_CASE).containsMatchIn(html)

  return when {
    hasHtmlTag && hasHeadTag -> {
      // Insert base and bridge right after <head ...>
      val headOpenRegex = Regex("<head[^>]*>", RegexOption.IGNORE_CASE)
      headOpenRegex.replace(html) { match ->
        match.value + baseTag + bridgeTag
      }
    }
    hasHtmlTag && hasBodyTag -> {
      // No head, insert at start of body
      val bodyOpenRegex = Regex("<body[^>]*>", RegexOption.IGNORE_CASE)
      bodyOpenRegex.replace(html) { match ->
        match.value + bridgeTag
      }
    }
    hasHtmlTag -> {
      // Has <html> but neither <head> nor <body>; create a head
      val htmlOpenRegex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
      htmlOpenRegex.replace(html) { match ->
        match.value + "<head>$baseTag$bridgeTag</head>"
      }
    }
    else -> {
      // Treat as fragment; wrap into a full document
      """
      <!DOCTYPE html>
      <html>
        <head>
          $baseTag
          $bridgeTag
        </head>
        <body>
          $html
        </body>
      </html>
      """.trimIndent()
    }
  }
}