package sample.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aryapreetam.cmpwebview.WebView
import io.github.aryapreetam.cmpwebview.WebViewController
import io.github.aryapreetam.cmpwebview.rememberWebViewController
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class WebViewBridgeIntegrationAndroidTest {

  @get:Rule
  val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun bidirectionalBridge_roundTrip_ack() {
    lateinit var controller: WebViewController
    val lastMessage = AtomicReference<String?>(null)
    val loadError = AtomicReference<String?>(null)

    rule.setContent {
      val c = rememberWebViewController()
      SideEffect { controller = c }

      WebView(
        htmlContent = CONTROLLED_HTML,
        modifier = Modifier,
        controller = c,
        onScriptResult = { lastMessage.set(it) },
        onLoadError = { loadError.set(it) },
      )
    }

    rule.waitUntil(timeoutMillis = 30_000) {
      loadError.get() != null || lastMessage.get() == "ready"
    }
    assertNull("WebView load error: ${loadError.get()}", loadError.get())

    runBlocking {
      controller.evaluateJavaScript("window.ComposeWebViewBridge.postMessage('ack');")
    }

    rule.waitUntil(timeoutMillis = 20_000) {
      loadError.get() != null || lastMessage.get() == "ack"
    }
    assertNull("WebView load error: ${loadError.get()}", loadError.get())
    assertEquals("ack", lastMessage.get())
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
