package screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Home screen with navigation to all demo screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (DemoScreen) -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("CMP WebView Demo") }
      )
    }
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      item {
        Text(
          text = "Welcome to CMP WebView Demo",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
          text = "Explore different WebView features across platforms",
          fontSize = 16.sp,
          modifier = Modifier.padding(bottom = 16.dp)
        )
      }

      item {
        Text(
          text = "Core Features",
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(vertical = 8.dp)
        )
      }

      items(DemoScreen.coreScreens) { screen ->
        DemoScreenCard(
          title = screen.title,
          description = screen.description,
          onClick = { onNavigate(screen) }
        )
      }

      item {
        Text(
          text = "Advanced Features",
          fontSize = 18.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
      }

      items(DemoScreen.advancedScreens) { screen ->
        DemoScreenCard(
          title = screen.title,
          description = screen.description,
          onClick = { onNavigate(screen) }
        )
      }
    }
  }
}

@Composable
private fun DemoScreenCard(
  title: String,
  description: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    onClick = onClick
  ) {
    Column(
      modifier = Modifier.padding(16.dp)
    ) {
      Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = description,
        fontSize = 14.sp
      )
    }
  }
}

sealed class DemoScreen(
  val title: String,
  val description: String,
  val route: String
) {
  object Home : DemoScreen("Home", "Main navigation screen", "home")
  object UrlLoading : DemoScreen("URL Loading", "Load remote web content", "url-loading")
  object HtmlLoading : DemoScreen("HTML Loading", "Display HTML content", "html-loading")
  object BridgeDemo : DemoScreen("JavaScript Bridge", "JS-Compose communication", "bridge")
  object ErrorHandling : DemoScreen("Error Handling", "Handle loading errors", "error-handling")
  object ContentSwitching : DemoScreen("Content Switching", "Switch between URL and HTML", "content-switch")
  object CustomHeaders : DemoScreen("Custom Headers", "Load with authentication headers", "custom-headers")
  object MultiWebView : DemoScreen("Multiple WebViews", "Multiple concurrent WebViews", "multi-webview")
  object LoadingState : DemoScreen("Loading States", "Custom loading indicators", "loading-state")

  companion object {
    val coreScreens = listOf(UrlLoading, HtmlLoading, BridgeDemo, ErrorHandling)
    val advancedScreens = listOf(ContentSwitching, CustomHeaders, MultiWebView, LoadingState)
    val allScreens = coreScreens + advancedScreens
  }
}
