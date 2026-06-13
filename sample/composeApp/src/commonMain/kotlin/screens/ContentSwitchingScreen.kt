package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import sample.app.getYouTubeUrl

/**
 * Demo screen for switching between URL and HTML content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSwitchingScreen(onBack: () -> Unit) {
  var contentMode by remember { mutableStateOf(ContentMode.URL) }
  var currentUrl by remember { mutableStateOf("https://example.com") }
  var isLoading by remember { mutableStateOf(false) }
  var switchCount by remember { mutableStateOf(0) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Content Switching Demo") },
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
      // Mode switcher
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Current Mode: ${'$'}{contentMode.name}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                contentMode = ContentMode.URL
                switchCount++
              },
              modifier = Modifier.weight(1f),
              enabled = contentMode != ContentMode.URL
            ) {
              Text("Switch to URL")
            }
            Button(
              onClick = {
                contentMode = ContentMode.HTML
                switchCount++
              },
              modifier = Modifier.weight(1f),
              enabled = contentMode != ContentMode.HTML
            ) {
              Text("Switch to HTML")
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Switches: $switchCount",
              style = MaterialTheme.typography.bodySmall
            )
            if (isLoading) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 1.5.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Loading...", style = MaterialTheme.typography.bodySmall)
              }
            }
          }
        }
      }

      // Content area
      Box(modifier = Modifier.fillMaxSize()) {
        when (contentMode) {
          ContentMode.URL -> {
            Column(modifier = Modifier.fillMaxSize()) {
              // URL selector
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedButton(
                  onClick = { currentUrl = "https://example.com" },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Example", maxLines = 1)
                }
                OutlinedButton(
                  onClick = { currentUrl = getYouTubeUrl("kIEBQ_czdxs") },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("YouTube", maxLines = 1)
                }
              }

              WebView(
                url = currentUrl,
                modifier = Modifier.fillMaxSize().testTag("content-switch-url-webview"),
                onLoadStarted = { isLoading = true },
                onLoadFinished = { isLoading = false },
                onLoadError = { isLoading = false }
              )
            }
          }

          ContentMode.HTML -> {
            WebView(
              htmlContent = """
                                <html>
                                    <head>
                                        <style>
                                            body { 
                                                font-family: system-ui, -apple-system, sans-serif;
                                                padding: 20px;
                                                background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
                                                color: white;
                                                display: flex;
                                                align-items: center;
                                                justify-content: center;
                                                min-height: 100vh;
                                                margin: 0;
                                            }
                                            .card {
                                                background: rgba(255,255,255,0.2);
                                                padding: 40px;
                                                border-radius: 20px;
                                                backdrop-filter: blur(10px);
                                                text-align: center;
                                            }
                                            h1 { font-size: 2.5em; margin: 0 0 20px 0; }
                                            p { font-size: 1.2em; margin: 10px 0; }
                                        </style>
                                    </head>
                                    <body>
                                        <div class="card">
                                            <h1>🎨 HTML Content</h1>
                                            <p>This is dynamically rendered HTML</p>
                                            <p>Switches: $switchCount</p>
                                            <p>Mode: ${contentMode.name}</p>
                                        </div>
                                    </body>
                                </html>
                            """.trimIndent(),
              modifier = Modifier.fillMaxSize().testTag("content-switch-html-webview"),
              onLoadStarted = { isLoading = true },
              onLoadFinished = { isLoading = false }
            )
          }
        }
      }
    }
  }
}

private enum class ContentMode {
  URL, HTML
}
