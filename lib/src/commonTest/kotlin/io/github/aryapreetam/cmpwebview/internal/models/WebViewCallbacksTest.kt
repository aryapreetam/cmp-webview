package io.github.aryapreetam.cmpwebview.internal.models

import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for WebViewCallbacks data class.
 */
class WebViewCallbacksTest {

  @Test
  fun `Empty callbacks should have all null callbacks`() {
    val callbacks = WebViewCallbacks.EMPTY
    assertNotNull(callbacks)
    assertNull(callbacks.onScriptResult)
    assertNull(callbacks.onLoadStarted)
    assertNull(callbacks.onLoadFinished)
    assertNull(callbacks.onLoadError)
  }

  @Test
  fun `Callbacks with all parameters should store them correctly`() {
    var scriptResultCalled = false
    var loadStartedCalled = false
    var loadFinishedCalled = false
    var loadErrorCalled = false

    val callbacks = WebViewCallbacks(
      onScriptResult = { scriptResultCalled = true },
      onLoadStarted = { loadStartedCalled = true },
      onLoadFinished = { loadFinishedCalled = true },
      onLoadError = { loadErrorCalled = true }
    )

    assertNotNull(callbacks.onScriptResult)
    assertNotNull(callbacks.onLoadStarted)
    assertNotNull(callbacks.onLoadFinished)
    assertNotNull(callbacks.onLoadError)

    // Verify callbacks can be invoked
    callbacks.onScriptResult?.invoke("test")
    callbacks.onLoadStarted?.invoke()
    callbacks.onLoadFinished?.invoke()
    callbacks.onLoadError?.invoke("error")

    assertEquals(true, scriptResultCalled)
    assertEquals(true, loadStartedCalled)
    assertEquals(true, loadFinishedCalled)
    assertEquals(true, loadErrorCalled)
  }

  @Test
  fun `Callbacks with partial parameters should store only provided ones`() {
    val callbacks = WebViewCallbacks(
      onScriptResult = { },
      onLoadStarted = null,
      onLoadFinished = { },
      onLoadError = null
    )

    assertNotNull(callbacks.onScriptResult)
    assertNull(callbacks.onLoadStarted)
    assertNotNull(callbacks.onLoadFinished)
    assertNull(callbacks.onLoadError)
  }

  @Test
  fun `Callbacks should correctly capture and pass message content`() {
    var receivedMessage = ""
    var receivedError = ""

    val callbacks = WebViewCallbacks(
      onScriptResult = { message -> receivedMessage = message },
      onLoadStarted = null,
      onLoadFinished = null,
      onLoadError = { error -> receivedError = error }
    )

    callbacks.onScriptResult?.invoke("Hello from JavaScript")
    callbacks.onLoadError?.invoke("Network error")

    assertEquals("Hello from JavaScript", receivedMessage)
    assertEquals("Network error", receivedError)
  }
}
