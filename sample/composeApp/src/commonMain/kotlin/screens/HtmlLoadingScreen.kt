package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/**
 * Demo screen for displaying HTML content with baseUrl
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlLoadingScreen(onBack: () -> Unit) {
  var selectedDemo by remember { mutableStateOf(HtmlDemo.BASIC) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("HTML Loading Demo") },
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
      // Demo selector
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Select HTML demo:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HtmlDemo.values().forEach { demo ->
              Button(
                onClick = { selectedDemo = demo },
                modifier = Modifier.weight(1f)
              ) {
                Text(demo.label, maxLines = 1)
              }
            }
          }

          Text(
            text = selectedDemo.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }

      // WebView
      WebView(
        htmlContent = selectedDemo.html,
        baseUrl = selectedDemo.baseUrl,
        modifier = Modifier.fillMaxSize().testTag("html-loading-webview")
      )
    }
  }
}

private enum class HtmlDemo(
  val label: String,
  val description: String,
  val html: String,
  val baseUrl: String? = null
) {
  BASIC(
    label = "Basic",
    description = "Simple HTML content",
    html = """
            <html>
                <head>
                    <style>
                        body { 
                            font-family: system-ui, -apple-system, sans-serif;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                        }
                        h1 { color: #fff; }
                        .card { 
                            background: rgba(255,255,255,0.1);
                            padding: 20px;
                            border-radius: 10px;
                            backdrop-filter: blur(10px);
                        }
                    </style>
                </head>
                <body>
                    <h1>Hello from HTML!</h1>
                    <div class="card">
                        <p>This content is rendered from an HTML string.</p>
                        <p>It supports:</p>
                        <ul>
                            <li>Custom styling</li>
                            <li>Interactive elements</li>
                            <li>JavaScript</li>
                        </ul>
                    </div>
                </body>
            </html>
        """.trimIndent()
  ),

  WITH_BASE_URL(
    label = "Base URL",
    description = "HTML with baseUrl for relative links",
    html = """
            <html>
                <head>
                    <style>
                        body { 
                            font-family: system-ui, -apple-system, sans-serif;
                            padding: 20px;
                        }
                        a { 
                            color: #667eea;
                            text-decoration: none;
                            padding: 10px;
                            display: block;
                            border: 1px solid #667eea;
                            border-radius: 5px;
                            margin: 10px 0;
                        }
                    </style>
                </head>
                <body>
                    <h1>BaseURL Demo</h1>
                    <p>These links are resolved relative to the baseUrl:</p>
                    <a href="page2.html">→ Relative Link (page2.html)</a>
                    <a href="/docs">→ Absolute Path (/docs)</a>
                    <p><small>BaseURL: https://example.com/</small></p>
                </body>
            </html>
        """.trimIndent(),
    baseUrl = "https://example.com/"
  ),

  INTERACTIVE(
    label = "Interactive",
    description = "HTML with JavaScript interactions",
    html = """
            <html>
                <head>
                    <style>
                        body { 
                            font-family: system-ui, -apple-system, sans-serif;
                            padding: 20px;
                            background: #f5f5f5;
                        }
                        button {
                            background: #667eea;
                            color: white;
                            border: none;
                            padding: 12px 24px;
                            border-radius: 6px;
                            font-size: 16px;
                            cursor: pointer;
                            margin: 5px;
                        }
                        button:hover { background: #5568d3; }
                        #counter {
                            font-size: 48px;
                            font-weight: bold;
                            color: #667eea;
                            margin: 20px 0;
                        }
                        .card {
                            background: white;
                            padding: 20px;
                            border-radius: 10px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Interactive HTML</h1>
                        <p>Click the buttons to interact:</p>
                        <div id="counter">0</div>
                        <button onclick="increment()">Increment</button>
                        <button onclick="decrement()">Decrement</button>
                        <button onclick="reset()">Reset</button>
                    </div>
                    
                    <script>
                        let count = 0;
                        function increment() {
                            count++;
                            document.getElementById('counter').textContent = count;
                        }
                        function decrement() {
                            count--;
                            document.getElementById('counter').textContent = count;
                        }
                        function reset() {
                            count = 0;
                            document.getElementById('counter').textContent = count;
                        }
                    </script>
                </body>
            </html>
        """.trimIndent()
  )
}
