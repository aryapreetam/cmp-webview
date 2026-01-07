package sample.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import io.github.aryapreetam.cmpwebview.WebView
import io.github.aryapreetam.cmpwebview.WebViewController
import io.github.aryapreetam.cmpwebview.rememberWebViewController
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.swing.JFrame
import javax.swing.SwingUtilities

class WebViewBridgeIntegrationJvmTest {

  @Test
  fun bidirectionalBridge_roundTrip_ack() {
    // NOTE: Desktop `runComposeUiTest` does not provide `LocalInteropContainer`, which is required
    // by `SwingPanel` (used by the Desktop WebView). So we run this as a Swing/ComposePanel-based
    // integration test instead.

    val controllerRef = AtomicReference<WebViewController?>(null)
    val lastMessage = AtomicReference<String?>(null)
    val loadError = AtomicReference<String?>(null)

    val readyLatch = CountDownLatch(1)
    val ackLatch = CountDownLatch(1)

    val frameRef = AtomicReference<JFrame?>(null)

    SwingUtilities.invokeAndWait {
      val frame = JFrame("cmp-webview integration test")
      frame.setSize(900, 700)
      frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE

      val panel = ComposePanel()
      panel.setContent {
        val controller = rememberWebViewController()
        SideEffect { controllerRef.set(controller) }

        WebView(
          htmlContent = CONTROLLED_HTML,
          modifier = Modifier.fillMaxSize(),
          controller = controller,
          onScriptResult = { msg ->
            lastMessage.set(msg)
            when (msg) {
              "ready" -> readyLatch.countDown()
              "ack" -> ackLatch.countDown()
            }
          },
          onLoadError = { err ->
            loadError.set(err)
            readyLatch.countDown()
            ackLatch.countDown()
          },
        )
      }

      frame.contentPane.add(panel)
      frame.isVisible = true
      frameRef.set(frame)
    }

    try {
      assertTrue(
        readyLatch.await(120, TimeUnit.SECONDS),
        "Timed out waiting for initial JS→Compose 'ready' message. lastMessage=${lastMessage.get()} error=${loadError.get()}"
      )
      assertNull(loadError.get(), "WebView load error: ${loadError.get()}")
      assertEquals("ready", lastMessage.get())

      // Desktop may not return values from `evaluateJavaScript`, so we validate execution by observing
      // a JS→Compose "ack" message.
      val controller = controllerRef.get()
      assertTrue(controller != null, "Controller was not attached")

      runBlocking {
        controller!!.evaluateJavaScript("window.ComposeWebViewBridge.postMessage('ack');")
      }

      assertTrue(
        ackLatch.await(30, TimeUnit.SECONDS),
        "Timed out waiting for JS→Compose 'ack' message. lastMessage=${lastMessage.get()} error=${loadError.get()}"
      )
      assertNull(loadError.get(), "WebView load error: ${loadError.get()}")
      assertEquals("ack", lastMessage.get())
    } finally {
      SwingUtilities.invokeAndWait {
        frameRef.getAndSet(null)?.dispose()
      }
    }
  }
}

private val CONTROLLED_HTML: String = """
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Bridge Integration Test</title>
  </head>
  <body>
    <p>Bridge integration test page</p>
    <script>
      window.addEventListener('ComposeWebViewBridgeReady', function () {
        window.ComposeWebViewBridge.postMessage('ready');
      });
    </script>
  </body>
</html>
""".trimIndent()
