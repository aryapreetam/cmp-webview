package io.github.aryapreetam.cmpwebview

sealed interface WebViewBridgeMode {
  /** Never inject scripts / never register message handlers. */
  data object Disabled : WebViewBridgeMode

  /** Enable JS→Compose only. */
  data object JsToCompose : WebViewBridgeMode

  /** Enable Compose→JS support when possible (and JS→Compose when actually used). */
  data object Bidirectional : WebViewBridgeMode

  /** Enable bridge only when it is actually used by the caller (callbacks/controller). */
  data object Auto : WebViewBridgeMode
}

data class WebViewOptions(
  val bridgeMode: WebViewBridgeMode = WebViewBridgeMode.Auto,
)
