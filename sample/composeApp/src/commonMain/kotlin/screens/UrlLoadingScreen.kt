package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView

/**
 * Demo screen for URL loading with different types of web content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlLoadingScreen(onBack: () -> Unit) {
  var currentUrl by remember { mutableStateOf("https://example.com") }
  var isLoading by remember { mutableStateOf(false) }
  var loadError by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("URL Loading Demo") },
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
      // URL selector buttons
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(
          modifier = Modifier.padding(16.dp)
        ) {
          Text(
            text = "Select a website to load:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { currentUrl = "https://example.com" },
              modifier = Modifier.weight(1f)
            ) {
              Text("Example", maxLines = 1)
            }
            Button(
              onClick = { currentUrl = "https://www.wikipedia.org" },
              modifier = Modifier.weight(1f)
            ) {
              Text("Wikipedia", maxLines = 1)
            }
            Button(
              onClick = { currentUrl = "https://github.com" },
              modifier = Modifier.weight(1f)
            ) {
              Text("GitHub", maxLines = 1)
            }
          }

          // Loading state indicator
          if (isLoading) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
              horizontalArrangement = Arrangement.Center,
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

          // Error display
          loadError?.let { error ->
            Text(
              text = "Error: $error",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 8.dp)
            )
          }
        }
      }

      // WebView
      Box(modifier = Modifier.fillMaxSize()) {
        WebView(
          url = currentUrl,
          modifier = Modifier.fillMaxSize(),
          testTag = "url-loading-webview",
          onLoadStarted = {
            isLoading = true
            loadError = null
          },
          onLoadFinished = {
            isLoading = false
            loadError = null
          },
          onLoadError = { error ->
            isLoading = false
            loadError = error
          }
        )
      }
    }
  }
}
