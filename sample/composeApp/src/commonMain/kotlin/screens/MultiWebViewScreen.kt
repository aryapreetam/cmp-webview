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
 * Demo screen showing multiple concurrent WebViews
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiWebViewScreen(onBack: () -> Unit) {
  var layoutMode by remember { mutableStateOf(LayoutMode.HORIZONTAL) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Multiple WebViews Demo") },
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
      // Layout mode selector
      Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Layout Mode:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { layoutMode = LayoutMode.HORIZONTAL },
              modifier = Modifier.weight(1f),
              enabled = layoutMode != LayoutMode.HORIZONTAL
            ) {
              Text("Horizontal")
            }
            Button(
              onClick = { layoutMode = LayoutMode.VERTICAL },
              modifier = Modifier.weight(1f),
              enabled = layoutMode != LayoutMode.VERTICAL
            ) {
              Text("Vertical")
            }
            Button(
              onClick = { layoutMode = LayoutMode.GRID },
              modifier = Modifier.weight(1f),
              enabled = layoutMode != LayoutMode.GRID
            ) {
              Text("Grid")
            }
          }

          Text(
            text = "Demonstrates proper isolation between WebView instances",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      }

      // WebView layout
      when (layoutMode) {
        LayoutMode.HORIZONTAL -> {
          Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            WebViewCard(
              url = "https://example.com",
              label = "Example",
              testTag = "webview-1",
              modifier = Modifier.weight(1f)
            )
            WebViewCard(
              url = "https://github.com",
              label = "GitHub",
              testTag = "webview-2",
              modifier = Modifier.weight(1f)
            )
          }
        }

        LayoutMode.VERTICAL -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            WebViewCard(
              url = "https://example.com",
              label = "Example",
              testTag = "webview-1",
              modifier = Modifier.weight(1f)
            )
            WebViewCard(
              url = "https://github.com",
              label = "GitHub",
              testTag = "webview-2",
              modifier = Modifier.weight(1f)
            )
          }
        }

        LayoutMode.GRID -> {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              WebViewCard(
                url = "https://example.com",
                label = "Example",
                testTag = "webview-1",
                modifier = Modifier.weight(1f)
              )
              WebViewCard(
                url = "https://github.com",
                label = "GitHub",
                testTag = "webview-2",
                modifier = Modifier.weight(1f)
              )
            }
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              WebViewCard(
                url = "https://www.wikipedia.org",
                label = "Wikipedia",
                testTag = "webview-3",
                modifier = Modifier.weight(1f)
              )
              WebViewCard(
                url = "https://httpbin.org/html",
                label = "HTTPBin",
                testTag = "webview-4",
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WebViewCard(
  url: String,
  label: String,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 2.dp
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(4.dp)
        )
      }
      WebView(
        url = url,
        modifier = Modifier.fillMaxSize().testTag(testTag)
      )
    }
  }
}

private enum class LayoutMode {
  HORIZONTAL, VERTICAL, GRID
}
