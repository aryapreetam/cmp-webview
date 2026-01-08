package sample.app

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test

class SampleUITest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun composeSmokeTest() = runComposeUiTest {
    setContent {
      Text("Sample UI test")
    }

    onNodeWithText("Sample UI test").assertExists()
  }
}
