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
 * Demo screen for URL loading with different types of web content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlLoadingScreen(onBack: () -> Unit) {
  var currentUrl by remember { mutableStateOf("https://example.com") }
  var isLoading by remember { mutableStateOf(false) }
  var loadError by remember { mutableStateOf<String?>(null) }
  var loadStatus by remember { mutableStateOf("Idle") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("URL Loading Demo") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
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
              onClick = {
                if (currentUrl != "https://example.com") {
                  currentUrl = "https://example.com"
                  loadStatus = "Loading"
                }
              },
              modifier = Modifier.weight(1f).testTag("btn_example")
            ) { Text("Example", maxLines = 1) }
            Button(
              onClick = {
                if (currentUrl != "https://html5test.co") {
                  currentUrl = "https://html5test.co"
                  loadStatus = "Loading"
                }
              },
              modifier = Modifier.weight(1f).testTag("btn_html5test")
            ) { Text("HTML5 Test", maxLines = 1) }
            Button(
              onClick = {
                val target = getYouTubeUrl("kIEBQ_czdxs")
                if (currentUrl != target) {
                  currentUrl = target
                  loadStatus = "Loading"
                }
              },
              modifier = Modifier.weight(1f).testTag("btn_youtube")
            ) { Text("YouTube", maxLines = 1) }
          }

          // Page status indicator
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Page Status: $loadStatus",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("page_status_indicator")
          )

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
          modifier = Modifier.fillMaxSize().testTag("url-loading-webview"),
          onLoadStarted = {
            loadStatus = "Loading"
            isLoading = true
            loadError = null
          },
          onLoadFinished = {
            loadStatus = "Loaded"
            isLoading = false
            loadError = null
          },
          onLoadError = { error ->
            loadStatus = "Error: $error"
            isLoading = false
            loadError = error
          }
        )
      }
    }
  }
}
