package sample.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import screens.HtmlLoadingScreen
import kotlin.test.Test

class SampleUITest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun htmlLoadingScreenTest() = runComposeUiTest {
    setContent {
      HtmlLoadingScreen(onBack = {})
    }

    // Verify the screen title is displayed
    onNodeWithText("HTML Loading Demo").assertExists()

    // Verify the demo selector text
    onNodeWithText("Select HTML demo:").assertExists()

    // Verify the WebView with testTag exists
    onNodeWithTag("html-loading-webview").assertExists()

    // Verify one of the demo buttons exists
    onNodeWithText("Basic").assertExists()
  }
}