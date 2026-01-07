package sample.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.material3.Text
import kotlin.test.Test

class SampleUITest {

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun composeSmokeTest() = runComposeUiTest {
    setContent {
      // NOTE:
      // WebView interop UI tests are not portable to iOS/WASM via `runComposeUiTest`
      // (see `docs-to-ignore/compose-multiplatform-interop-testing.md`).
      // Keep this common test as a lightweight Compose test harness sanity check.
      Text("Sample UI test")
    }

    onNodeWithText("Sample UI test").assertExists()
  }
}