package screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aryapreetam.cmpwebview.WebView
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import sample.app.getYouTubeUrl

/**
 * Demo screen for different loading state patterns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingStateScreen(onBack: () -> Unit) {
  var currentUrl by remember { mutableStateOf("https://example.com") }
  var isLoading by remember { mutableStateOf(false) }
  var loadProgress by remember { mutableStateOf(0f) }
  var showWebView by remember { mutableStateOf(true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Loading States Demo") },
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
      // Controls
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Load different URLs:",
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
              Text("Example")
            }
            Button(
              onClick = { currentUrl = getYouTubeUrl("kIEBQ_czdxs") },
              modifier = Modifier.weight(1f)
            ) {
              Text("YouTube")
            }
            Button(
              onClick = { currentUrl = "https://www.wikipedia.org" },
              modifier = Modifier.weight(1f)
            ) {
              Text("Wikipedia")
            }
          }

          HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

          Button(
            onClick = {
              showWebView = false
              // Simulate refresh
              kotlinx.coroutines.GlobalScope.launch {
                kotlinx.coroutines.delay(100)
                showWebView = true
              }
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("🔄 Pull to Refresh (Simulated)")
          }

          // Loading state indicators
          if (isLoading) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Loading...",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.primary
                )
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
              }

              // Simulated progress bar
              LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 8.dp)
              )
            }
          } else {
            Text(
              text = "✓ Loaded",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 12.dp)
            )
          }
        }
      }

      // WebView with loading overlay
      Box(modifier = Modifier.fillMaxSize()) {
        if (showWebView) {
          WebView(
            url = currentUrl,
            modifier = Modifier.fillMaxSize().testTag("loading-state-webview"),
            onLoadStarted = {
              isLoading = true
              loadProgress = 0f
              // Simulate progress
              kotlinx.coroutines.GlobalScope.launch {
                for (i in 1..10) {
                  kotlinx.coroutines.delay(100)
                  loadProgress = i / 10f
                }
              }
            },
            onLoadFinished = {
              isLoading = false
              loadProgress = 1f
            },
            onLoadError = { error ->
              isLoading = false
              loadProgress = 0f
            }
          )
        }

        // Full-screen loading overlay (shown on first load)
        if (isLoading && loadProgress < 0.3f) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
          ) {
            Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Loading content...",
                style = MaterialTheme.typography.bodyLarge
              )
            }
          }
        }
      }
    }
  }
}
