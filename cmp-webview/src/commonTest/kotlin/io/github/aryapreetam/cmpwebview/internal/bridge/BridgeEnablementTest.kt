package io.github.aryapreetam.cmpwebview.internal.bridge

import io.github.aryapreetam.cmpwebview.WebViewBridgeMode
import io.github.aryapreetam.cmpwebview.WebViewControllerImpl
import io.github.aryapreetam.cmpwebview.WebViewOptions
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeEnablementTest {

  @Test
  fun `auto enables nothing when unused`() {
    val enablement = resolveBridgeEnablement(
      options = WebViewOptions(bridgeMode = WebViewBridgeMode.Auto),
      callbacks = WebViewCallbacks(
        onScriptResult = null,
        onLoadStarted = null,
        onLoadFinished = null,
        onLoadError = null
      ),
      controller = null
    )

    assertEquals(BridgeEnablement(jsToCompose = false, composeToJs = false), enablement)
  }

  @Test
  fun `auto enables jsToCompose when callback is provided`() {
    val enablement = resolveBridgeEnablement(
      options = WebViewOptions(bridgeMode = WebViewBridgeMode.Auto),
      callbacks = WebViewCallbacks(
        onScriptResult = { },
        onLoadStarted = null,
        onLoadFinished = null,
        onLoadError = null
      ),
      controller = null
    )

    assertEquals(BridgeEnablement(jsToCompose = true, composeToJs = false), enablement)
  }

  @Test
  fun `auto enables composeToJs when controller is provided`() {
    val controller = WebViewControllerImpl()
    val enablement = resolveBridgeEnablement(
      options = WebViewOptions(bridgeMode = WebViewBridgeMode.Auto),
      callbacks = WebViewCallbacks(
        onScriptResult = null,
        onLoadStarted = null,
        onLoadFinished = null,
        onLoadError = null
      ),
      controller = controller
    )

    assertEquals(BridgeEnablement(jsToCompose = false, composeToJs = true), enablement)
  }

  @Test
  fun `jsToCompose mode only enables jsToCompose`() {
    val controller = WebViewControllerImpl()
    val enablement = resolveBridgeEnablement(
      options = WebViewOptions(bridgeMode = WebViewBridgeMode.JsToCompose),
      callbacks = WebViewCallbacks(
        onScriptResult = { },
        onLoadStarted = null,
        onLoadFinished = null,
        onLoadError = null
      ),
      controller = controller
    )

    assertEquals(BridgeEnablement(jsToCompose = true, composeToJs = false), enablement)
  }

  @Test
  fun `bidirectional enables composeToJs when controller is provided`() {
    val controller = WebViewControllerImpl()
    val enablement = resolveBridgeEnablement(
      options = WebViewOptions(bridgeMode = WebViewBridgeMode.Bidirectional),
      callbacks = WebViewCallbacks(
        onScriptResult = null,
        onLoadStarted = null,
        onLoadFinished = null,
        onLoadError = null
      ),
      controller = controller
    )

    assertEquals(BridgeEnablement(jsToCompose = false, composeToJs = true), enablement)
  }
}
