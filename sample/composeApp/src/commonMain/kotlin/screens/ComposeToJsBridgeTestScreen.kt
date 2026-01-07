package screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView
import io.github.aryapreetam.cmpwebview.WebViewJsResult
import io.github.aryapreetam.cmpwebview.rememberWebViewController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeToJsBridgeTestScreen(onBack: () -> Unit = {}) {
  val controller = rememberWebViewController()
  val scope = rememberCoroutineScope()

  var lastEval by remember { mutableStateOf("No Compose→JS calls yet") }
  var pageLoaded by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Compose→JS (Basic)") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Call JavaScript from Compose using controller.evaluateJavaScript(...)",
            style = MaterialTheme.typography.bodyMedium
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              modifier = Modifier.weight(1f),
              enabled = pageLoaded,
              onClick = {
                scope.launch {
                  lastEval = "Running…"
                  lastEval = formatEvalResult(
                    controller.evaluateJavaScript(
                      "document.title = 'Title set by Compose'; document.title"
                    )
                  )
                }
              }
            ) {
              Text("Set title")
            }

            Button(
              modifier = Modifier.weight(1f),
              enabled = pageLoaded,
              onClick = {
                scope.launch {
                  lastEval = "Running…"
                  lastEval = formatEvalResult(controller.evaluateJavaScript("document.title"))
                }
              }
            ) {
              Text("Get title")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = pageLoaded,
            onClick = {
              scope.launch {
                lastEval = "Running…"
                lastEval = formatEvalResult(
                  controller.evaluateJavaScript(
                    "(() => { const el = document.getElementById('box'); if (!el) return 'box-missing'; el.style.background = 'tomato'; return 'ok'; })()"
                  )
                )
              }
            }
          ) {
            Text("Change box color")
          }

          if (!pageLoaded) {
            Text(
              text = "Waiting for page to load…",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 8.dp)
            )
          }

          Text(
            text = "Result: $lastEval",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }

      WebView(
        htmlContent = composeToJsHtml,
        modifier = Modifier.fillMaxSize(),
        controller = controller,
        onLoadStarted = { pageLoaded = false },
        onLoadFinished = { pageLoaded = true },
        onLoadError = {
          pageLoaded = false
          lastEval = "Load error: $it"
        }
      )
    }
  }
}

private fun formatEvalResult(result: WebViewJsResult): String {
  return when (result) {
    is WebViewJsResult.Success -> "Success(${result.rawJsonOrString})"
    is WebViewJsResult.Unsupported -> "Unsupported(${result.reason})"
    is WebViewJsResult.Error -> "Error(${result.message})"
  }
}

private val composeToJsHtml: String = """
<!DOCTYPE html>
<html>
  <head>
    <meta charset=\"UTF-8\" />
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />
    <title>Compose→JS Demo</title>
    <style>
      body { font-family: sans-serif; padding: 16px; }
      #box { width: 100%; height: 120px; background: #4CAF50; border-radius: 12px; }
    </style>
  </head>
  <body>
    <h3>Compose→JS demo page</h3>
    <div id=\"box\"></div>
    <p>This page is controlled by Compose calling JavaScript.</p>
  </body>
</html>
""".trimIndent()
