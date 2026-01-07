package io.github.aryapreetam.cmpwebview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class WebViewControllerImplTest {

  @Test
  fun `evaluateJavaScript returns Unsupported when not attached`() = runBlocking {
    val controller = WebViewControllerImpl()

    val result = controller.evaluateJavaScript("1 + 1")

    assertTrue(result is WebViewJsResult.Unsupported)
  }

  @Test
  fun `controller forwards calls to attached bindings`() = runBlocking {
    val controller = WebViewControllerImpl()

    var reloadCalled = false
    var goBackCalled = false
    var goForwardCalled = false

    controller.attach(
      WebViewControllerImpl.Bindings(
        evaluateJavaScript = { WebViewJsResult.Success("ok") },
        reload = { reloadCalled = true },
        goBack = {
          goBackCalled = true
          true
        },
        goForward = {
          goForwardCalled = true
          false
        }
      )
    )

    assertEquals(WebViewJsResult.Success("ok"), controller.evaluateJavaScript("ignored"))

    controller.reload()
    assertTrue(reloadCalled)

    assertTrue(controller.goBack())
    assertTrue(goBackCalled)

    assertFalse(controller.goForward())
    assertTrue(goForwardCalled)
  }

  @Test
  fun `detach clears bindings`() = runBlocking {
    val controller = WebViewControllerImpl()
    controller.attach(
      WebViewControllerImpl.Bindings(
        evaluateJavaScript = { WebViewJsResult.Success("ok") },
        reload = { },
        goBack = { true },
        goForward = { true }
      )
    )

    controller.detach()
    val result = controller.evaluateJavaScript("ignored")
    assertTrue(result is WebViewJsResult.Unsupported)
  }
}
