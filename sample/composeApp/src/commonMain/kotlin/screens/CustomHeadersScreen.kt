package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView

/**
 * Demo screen for loading URLs with custom headers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomHeadersScreen(onBack: () -> Unit) {
  var selectedDemo by remember { mutableStateOf(HeaderDemo.BASIC) }
  var isLoading by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Custom Headers Demo") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Text("←", style = MaterialTheme.typography.headlineMedium)
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
            text = "Select header scenario:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            HeaderDemo.values().forEach { demo ->
              Button(
                onClick = { selectedDemo = demo },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(demo.label)
              }
            }
          }

          Text(
            text = selectedDemo.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
          )

          if (isLoading) {
            Row(
              modifier = Modifier.padding(top = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Loading...", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }

      // WebView
      WebView(
        url = selectedDemo.url,
        headers = selectedDemo.headers,
        modifier = Modifier.fillMaxSize().testTag("custom-headers-webview"),
        onLoadStarted = { isLoading = true },
        onLoadFinished = { isLoading = false },
        onLoadError = { isLoading = false }
      )
    }
  }
}

private enum class HeaderDemo(
  val label: String,
  val description: String,
  val url: String,
  val headers: Map<String, String>
) {
  BASIC(
    label = "No Headers",
    description = "Standard request without custom headers",
    url = "https://httpbin.org/headers",
    headers = emptyMap()
  ),
  USER_AGENT(
    label = "Custom User-Agent",
    description = "Request with custom User-Agent header",
    url = "https://httpbin.org/user-agent",
    headers = mapOf("User-Agent" to "CMP-WebView-Demo/1.0")
  ),
  AUTH_BEARER(
    label = "Authorization Header",
    description = "Request with Bearer token (simulated)",
    url = "https://httpbin.org/headers",
    headers = mapOf("Authorization" to "Bearer demo_token_12345")
  ),
  MULTIPLE(
    label = "Multiple Headers",
    description = "Request with multiple custom headers",
    url = "https://httpbin.org/headers",
    headers = mapOf(
      "User-Agent" to "CMP-WebView-Demo/1.0",
      "X-Custom-Header" to "CustomValue",
      "Accept-Language" to "en-US"
    )
  )
}
