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
import io.github.aryapreetam.cmpwebview.internal.bridge.resolveBridgeEnablement
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.coroutines.resume
import kotlin.js.toJsString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
) {
  val latestCallbacks by rememberUpdatedState(callbacks)

  val bridgeEnablement = remember(options, callbacks.onScriptResult, controller) {
    resolveBridgeEnablement(options, callbacks, controller)
  }
  val jsToComposeEnabled = bridgeEnablement.jsToCompose
  val composeToJsEnabled = bridgeEnablement.composeToJs && options.bridgeMode != WebViewBridgeMode.Disabled

  // Controller is best-effort and only fully supported for srcdoc (htmlContent). For cross-origin url iframes,
  // JS evaluation is blocked by browser security boundaries.
  val pendingEval = remember { mutableMapOf<String, (WebViewJsResult) -> Unit>() }
  var evalSeq by remember { mutableStateOf(0) }

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

  DisposableEffect(controller, composeToJsEnabled) {
    val impl = controller as? WebViewControllerImpl
    if (impl != null) {
      impl.attach(
        WebViewControllerImpl.Bindings(
          evaluateJavaScript = { script ->
            val isUrlLoad = currentUrl != null
            if (!composeToJsEnabled) {
              return@Bindings WebViewJsResult.Unsupported(
                "WASM evaluateJavaScript is disabled by WebViewOptions.bridgeMode=Disabled"
              )
            }
            if (isUrlLoad) {
              return@Bindings WebViewJsResult.Unsupported(
                "WASM evaluateJavaScript is best-effort and is not supported for url iframes (cross-origin isolation blocks evaluation)"
              )
            }

            val targetWindow = iframe.contentWindow
              ?: return@Bindings WebViewJsResult.Unsupported("WASM iframe is not ready")

            suspendCancellableCoroutine { continuation ->
              val id = (++evalSeq).toString()

              pendingEval[id] = { result ->
                if (continuation.isActive) {
                  continuation.resume(result)
                }
              }
              continuation.invokeOnCancellation {
                pendingEval.remove(id)
              }

              val request = buildEvalRequest(id, script)
              // srcdoc has an opaque origin; targetOrigin must be "*".
              targetWindow.postMessage(request.toJsString(), "*")
            }
          },
          reload = {
            // Best-effort no-op for now; platform-specific evaluation/navigation is implemented in T032.
          },
          goBack = { false },
          goForward = { false }
        )
      )
    }

    onDispose {
      impl?.detach()
    }
  }

  // Lifecycle: always attach load/error handlers; attach message handler only when bridge is enabled.
  DisposableEffect(Unit) {
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
      iframe.removeEventListener("load", loadHandler)
      iframe.removeEventListener("error", errorHandler)
    }
  }

  if (jsToComposeEnabled || composeToJsEnabled) {
    DisposableEffect(jsToComposeEnabled, composeToJsEnabled) {
      val messageHandler: (Event) -> Unit = messageHandler@{ event ->
        val messageEvent = event as? MessageEvent ?: return@messageHandler
        val expectedSource = iframe.contentWindow
        if (expectedSource == null || messageEvent.source != expectedSource) return@messageHandler

        val raw = messageEvent.data?.toString() ?: "null"

        if (jsToComposeEnabled) {
          // JS→Compose bridge payloads (ignore internal eval messages).
          val isEvalMessage = raw.startsWith(EVAL_REQUEST_PREFIX) ||
            raw.startsWith(EVAL_RESULT_PREFIX) ||
            raw.startsWith(EVAL_ERROR_PREFIX)
          if (!isEvalMessage) {
            latestCallbacks.onScriptResult?.invoke(unwrapBridgeMessage(raw))
          }
        }

        if (composeToJsEnabled) {
          val evalResult = parseEvalResponse(raw)
          if (evalResult != null) {
            val resume = pendingEval.remove(evalResult.id)
            resume?.invoke(evalResult.result)
          }
        }
      }
      window.addEventListener("message", messageHandler)

      onDispose {
        window.removeEventListener("message", messageHandler)
        if (!composeToJsEnabled) {
          pendingEval.clear()
        }
      }
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
        // Build HTML with optional base URL and any required injected scripts
        val htmlWithBridge = buildHtmlForSrcDoc(
          html = content.htmlContent,
          baseUrl = content.baseUrl,
          includeJsToComposeBridge = jsToComposeEnabled,
          includeEvalSupport = composeToJsEnabled,
        )
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
private fun buildHtmlForSrcDoc(
  html: String,
  baseUrl: String?,
  includeJsToComposeBridge: Boolean,
  includeEvalSupport: Boolean,
): String {
  val bridgeTag = if (includeJsToComposeBridge) {
    """
      <script>
      $BRIDGE_SCRIPT
      </script>
    """.trimIndent()
  } else {
    ""
  }

  val evalTag = if (includeEvalSupport) {
    """
      <script>
      $EVAL_LISTENER_SCRIPT
      </script>
    """.trimIndent()
  } else {
    ""
  }

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
        match.value + baseTag + bridgeTag + evalTag
      }
    }
    hasHtmlTag && hasBodyTag -> {
      // No head, insert at start of body
      val bodyOpenRegex = Regex("<body[^>]*>", RegexOption.IGNORE_CASE)
      bodyOpenRegex.replace(html) { match ->
        match.value + bridgeTag + evalTag
      }
    }
    hasHtmlTag -> {
      // Has <html> but neither <head> nor <body>; create a head
      val htmlOpenRegex = Regex("<html[^>]*>", RegexOption.IGNORE_CASE)
      htmlOpenRegex.replace(html) { match ->
        match.value + "<head>$baseTag$bridgeTag$evalTag</head>"
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
          $evalTag
        </head>
        <body>
          $html
        </body>
      </html>
      """.trimIndent()
    }
  }
}

private const val EVAL_REQUEST_PREFIX = "__CMP_WEBVIEW_EVAL_V1__:"
private const val EVAL_RESULT_PREFIX = "__CMP_WEBVIEW_EVAL_RESULT_V1__:"
private const val EVAL_ERROR_PREFIX = "__CMP_WEBVIEW_EVAL_ERROR_V1__:"

private fun buildEvalRequest(id: String, script: String): String {
  return "$EVAL_REQUEST_PREFIX$id:${script.length}:$script"
}

private data class ParsedEvalResponse(val id: String, val result: WebViewJsResult)

private fun parseEvalResponse(message: String): ParsedEvalResponse? {
  val (prefix, isError) = when {
    message.startsWith(EVAL_RESULT_PREFIX) -> EVAL_RESULT_PREFIX to false
    message.startsWith(EVAL_ERROR_PREFIX) -> EVAL_ERROR_PREFIX to true
    else -> return null
  }

  val rest = message.substring(prefix.length)
  val first = rest.indexOf(':')
  if (first <= 0) return null
  val id = rest.substring(0, first)

  val rest2 = rest.substring(first + 1)
  val second = rest2.indexOf(':')
  if (second < 0) return null

  val lenStr = rest2.substring(0, second)
  val payload = rest2.substring(second + 1)
  val len = lenStr.toIntOrNull() ?: return null

  val result = if (isError) {
    if (len >= 0 && payload.length != len) return null
    WebViewJsResult.Error(payload, null)
  } else {
    if (len == -1) {
      WebViewJsResult.Success(null)
    } else {
      if (payload.length != len) return null
      WebViewJsResult.Success(payload)
    }
  }

  return ParsedEvalResponse(id = id, result = result)
}

// Minimal evaluator inside the iframe (only injected for srcdoc when Compose→JS is enabled).
// - Validates the sender is the parent window.
// - Parses a simple length-delimited message format.
// - Returns best-effort results via JSON.stringify when possible.
private const val EVAL_LISTENER_SCRIPT = """
(function() {
  const REQ = '${EVAL_REQUEST_PREFIX}';
  const RES = '${EVAL_RESULT_PREFIX}';
  const ERR = '${EVAL_ERROR_PREFIX}';

  window.addEventListener('message', function(event) {
    if (event.source !== window.parent) return;
    const data = (event && event.data != null) ? String(event.data) : 'null';
    if (!data.startsWith(REQ)) return;

    const rest = data.substring(REQ.length);
    const i1 = rest.indexOf(':');
    if (i1 <= 0) return;
    const id = rest.substring(0, i1);

    const rest2 = rest.substring(i1 + 1);
    const i2 = rest2.indexOf(':');
    if (i2 < 0) return;
    const lenStr = rest2.substring(0, i2);
    const script = rest2.substring(i2 + 1);

    const expectedLen = parseInt(lenStr, 10);
    if (!Number.isFinite(expectedLen)) return;
    if (script.length !== expectedLen) return;

    try {
      const r = eval(script);
      let payload = null;
      if (typeof r !== 'undefined') {
        try {
          payload = JSON.stringify(r);
          if (typeof payload === 'undefined') {
            payload = String(r);
          }
        } catch (e) {
          payload = String(r);
        }
      }

      const out = (payload === null)
        ? (RES + id + ':-1:')
        : (RES + id + ':' + payload.length + ':' + payload);
      window.parent.postMessage(out, '*');
    } catch (e) {
      const msg = (e && e.message) ? String(e.message) : String(e);
      const out = ERR + id + ':' + msg.length + ':' + msg;
      window.parent.postMessage(out, '*');
    }
  }, false);
})();
"""

// JS interop helpers for idempotent navigation at DOM level
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(el, url) => { const prev = (el.dataset && el.dataset.cmpwvUrl) || null; if (prev === url) return false; if (el.dataset) el.dataset.cmpwvUrl = url; if (el.hasAttribute('srcdoc')) el.removeAttribute('srcdoc'); el.src = url; return true; }")
private external fun setUrlIfChangedJs(el: HTMLIFrameElement, url: String): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(el, html) => { const h = String(html); const prev = (el.dataset && el.dataset.cmpwvHtml) || null; if (prev === h) return false; if (el.dataset) el.dataset.cmpwvHtml = h; el.removeAttribute('src'); el.srcdoc = h; return true; }")
private external fun setSrcDocIfChangedJs(el: HTMLIFrameElement, html: String): Boolean
