package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
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
  val latestCallbacks by rememberUpdatedState(callbacks)

  // Create the iframe once and let Compose manage its presence in the DOM
  val iframe = remember {
    (document.createElement("iframe") as HTMLIFrameElement).apply {
      style.width = "100%"
      style.height = "100%"
      style.border = "none"
    }
  }

  // Track only the current URL to avoid redundant loads
  var currentUrl by remember { mutableStateOf<String?>(null) }

  // Lifecycle: attach/remove global and element listeners once
  DisposableEffect(Unit) {
    // Message listener for bridge communication (JS bridge messages)
    val messageHandler: (Event) -> Unit = messageHandler@{ event ->
      val messageEvent = event as? MessageEvent ?: return@messageHandler
      val expectedSource = iframe.contentWindow
      if (expectedSource == null || messageEvent.source != expectedSource) return@messageHandler
      val rawMessage = messageEvent.data?.toString() ?: "null"
      latestCallbacks.onScriptResult?.invoke(unwrapBridgeMessage(rawMessage))
    }
    window.addEventListener("message", messageHandler)

    // Load finished callback
    val loadHandler: (Event) -> Unit = {
      latestCallbacks.onLoadFinished?.invoke()
    }
    iframe.addEventListener("load", loadHandler)

    // Error handler
    val errorHandler: (Event) -> Unit = {
      latestCallbacks.onLoadError?.invoke("Failed to load content")
    }
    iframe.addEventListener("error", errorHandler)

    onDispose {
      window.removeEventListener("message", messageHandler)
      iframe.removeEventListener("load", loadHandler)
      iframe.removeEventListener("error", errorHandler)
    }
  }

  // Use LaunchedEffect to handle navigation when content changes
  // Key on the actual URL/HTML string to prevent duplicate launches
  val contentKey = remember(content) {
    when (content) {
      is WebViewContent.Url -> "url:${content.url}"
      is WebViewContent.Html -> "html:${content.hashCode()}"
    }
  }

  LaunchedEffect(contentKey) {
    when (content) {
      is WebViewContent.Url -> {
        val targetUrl = content.url
        // Only navigate if URL actually changed (DOM-level guard)
        if (currentUrl != targetUrl) {
          currentUrl = targetUrl
          // Attempt to set URL; will be a no-op if same as last
          if (setUrlIfChangedJs(iframe, targetUrl)) {
            latestCallbacks.onLoadStarted?.invoke()
          }
        }
      }
      is WebViewContent.Html -> {
        currentUrl = null
        // Build HTML with optional base URL and bridge script
        val htmlWithBridge = buildHtmlForSrcDoc(content.htmlContent, content.baseUrl)
        // Attempt to set srcdoc; will be a no-op if same as last
        if (setSrcDocIfChangedJs(iframe, htmlWithBridge)) {
          latestCallbacks.onLoadStarted?.invoke()
        }
      }
    }
  }

  WebElementView(
    factory = { iframe },
    modifier = modifier,
    update = { /* content changes handled by LaunchedEffect(contentKey) */ }
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
          $BRIDGE_SCRIPT
        </head>
        <body>
          $html
        </body>
      </html>
      """.trimIndent()
    }
  }
}

// JS interop helpers for idempotent navigation at DOM level
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(el, url) => { const prev = (el.dataset && el.dataset.cmpwvUrl) || null; if (prev === url) return false; if (el.dataset) el.dataset.cmpwvUrl = url; if (el.hasAttribute('srcdoc')) el.removeAttribute('srcdoc'); el.src = url; return true; }")
private external fun setUrlIfChangedJs(el: HTMLIFrameElement, url: String): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(el, html) => { const h = String(html); const prev = (el.dataset && el.dataset.cmpwvHtml) || null; if (prev === h) return false; if (el.dataset) el.dataset.cmpwvHtml = h; el.removeAttribute('src'); el.srcdoc = h; return true; }")
private external fun setSrcDocIfChangedJs(el: HTMLIFrameElement, html: String): Boolean
