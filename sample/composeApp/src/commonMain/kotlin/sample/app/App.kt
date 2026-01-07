package sample.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import screens.*

@Composable
fun App() {
  var currentScreen by remember { mutableStateOf<DemoScreen>(DemoScreen.Home) }

  Box(
    modifier = Modifier.fillMaxSize().background(Color.White),
    contentAlignment = Alignment.Center
  ) {
    when (currentScreen) {
      DemoScreen.Home -> HomeScreen(onNavigate = { screen -> currentScreen = screen })
      DemoScreen.UrlLoading -> UrlLoadingScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.HtmlLoading -> HtmlLoadingScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.BridgeDemo -> BridgeDemoScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.ComposeToJsBridgeTest -> ComposeToJsBridgeTestScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.ErrorHandling -> ErrorHandlingScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.ContentSwitching -> ContentSwitchingScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.CustomHeaders -> CustomHeadersScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.MultiWebView -> MultiWebViewScreen(onBack = { currentScreen = DemoScreen.Home })
      DemoScreen.LoadingState -> LoadingStateScreen(onBack = { currentScreen = DemoScreen.Home })
    }
  }
}