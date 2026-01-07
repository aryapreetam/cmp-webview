package io.github.aryapreetam.cmpwebview.internal.bridge

import io.github.aryapreetam.cmpwebview.WebViewBridgeMode
import io.github.aryapreetam.cmpwebview.WebViewController
import io.github.aryapreetam.cmpwebview.WebViewOptions
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks

internal data class BridgeEnablement(
  val jsToCompose: Boolean,
  val composeToJs: Boolean,
)

internal fun resolveBridgeEnablement(
  options: WebViewOptions,
  callbacks: WebViewCallbacks,
  controller: WebViewController?,
): BridgeEnablement {
  val jsToCompose = when (options.bridgeMode) {
    WebViewBridgeMode.Disabled -> false
    WebViewBridgeMode.Auto -> callbacks.onScriptResult != null
    WebViewBridgeMode.JsToCompose -> callbacks.onScriptResult != null
    WebViewBridgeMode.Bidirectional -> callbacks.onScriptResult != null
  }

  val composeToJs = when (options.bridgeMode) {
    WebViewBridgeMode.Disabled -> false
    WebViewBridgeMode.Auto -> controller != null
    WebViewBridgeMode.JsToCompose -> false
    WebViewBridgeMode.Bidirectional -> controller != null
  }

  return BridgeEnablement(jsToCompose = jsToCompose, composeToJs = composeToJs)
}
