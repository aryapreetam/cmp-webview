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

/**
 * Demo screen for error handling scenarios
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorHandlingScreen(onBack: () -> Unit) {
  var selectedScenario by remember { mutableStateOf(ErrorScenario.NETWORK_ERROR) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var retryCount by remember { mutableStateOf(0) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Error Handling Demo") },
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
      // Scenario selector
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Select error scenario:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            ErrorScenario.values().forEach { scenario ->
              Button(
                onClick = {
                  selectedScenario = scenario
                  errorMessage = null
                  retryCount = 0
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(scenario.label)
              }
            }
          }

          Text(
            text = selectedScenario.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }

      // Content area
      Box(modifier = Modifier.fillMaxSize()) {
        if (errorMessage != null) {
          // Error state
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "⚠️ Load Error",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = errorMessage ?: "Unknown error",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
              )
              Button(
                onClick = {
                  errorMessage = null
                  retryCount++
                }
              ) {
                Text("Retry (Attempt ${retryCount + 1})")
              }
            }
          }
        } else {
          // WebView with loading indicator
          Box(modifier = Modifier.fillMaxSize()) {
            WebView(
              url = selectedScenario.url,
              modifier = Modifier.fillMaxSize().testTag("error-handling-webview"),
              onLoadStarted = {
                isLoading = true
              },
              onLoadFinished = {
                isLoading = false
              },
              onLoadError = { error ->
                isLoading = false
                errorMessage = error
              }
            )

            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
              )
            }
          }
        }
      }
    }
  }
}

private enum class ErrorScenario(
  val label: String,
  val description: String,
  val url: String
) {
  NETWORK_ERROR(
    label = "Network Error",
    description = "Unreachable host - simulates network failure",
    url = "https://this-domain-definitely-does-not-exist-12345.com"
  ),
  INVALID_URL(
    label = "Invalid URL",
    description = "Malformed URL - should be caught by validation",
    url = "not-a-valid-url"
  ),
  NOT_FOUND(
    label = "404 Not Found",
    description = "Valid host but page doesn't exist",
    url = "https://httpstat.us/404"
  ),
  SERVER_ERROR(
    label = "500 Server Error",
    description = "Server returns internal error",
    url = "https://httpstat.us/500"
  ),
  TIMEOUT(
    label = "Request Timeout",
    description = "Request takes too long to respond",
    url = "https://httpstat.us/524"
  )
}
