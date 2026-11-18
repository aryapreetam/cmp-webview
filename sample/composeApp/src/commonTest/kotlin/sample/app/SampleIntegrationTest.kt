package sample.app

import io.github.aryapreetam.parikshan.E2ETestLifecycle
import io.github.aryapreetam.parikshan.E2ETestScope
import io.github.aryapreetam.parikshan.InternalParikshanApi
import io.github.aryapreetam.parikshan.e2eTest
import kotlin.test.Test

class SampleIntegrationTest : E2ETestLifecycle {

  override suspend fun E2ETestScope.beforeEach() {
    try {
      if (hasVisibleNode("back_button")) {
        click("back_button")
      }
    } catch (_: Throwable) {
      // Best-effort recovery
    }
  }

  override suspend fun E2ETestScope.afterEach() {
    try {
      if (hasVisibleNode("back_button")) {
        click("back_button")
      }
    } catch (_: Throwable) {
      // Best-effort cleanup
    }
  }

  @OptIn(InternalParikshanApi::class)
  private suspend fun E2ETestScope.waitForLoaded(timeoutMs: Long = 25_000L) {
    executeParallel {
      waitFor("Page Status: Loaded", timeoutMs = timeoutMs)
    }
    assertVisible("Page Status: Loaded")
  }

  /**
   * Test 1: HTML Loading Demo — verifies tabs (Basic, Base URL, Interactive) and navigation.
   */
  @Test
  fun testHtmlLoadingWorkflow() = e2eTest {
    // 1. Verify Home Screen loads with header and sections
    assertVisible("CMP WebView Demo")
    assertVisible("Welcome to CMP WebView Demo")
    assertVisible("Core Features")
    assertVisible("demo_card_HTML Loading")

    // 2. Navigate to HTML Loading Demo
    click("demo_card_HTML Loading")

    // 3. Verify HTML Loading screen and demo selector are mounted
    assertVisible("HTML Loading Demo")
    assertVisible("Select HTML demo:")
    assertVisible("demo_tab_Basic")
    assertVisible("demo_tab_Base URL")
    assertVisible("demo_tab_Interactive")
    assertVisible("html-loading-webview")

    // 4. Switch to Base URL tab
    click("demo_tab_Base URL")
    assertVisible("HTML with baseUrl for relative links")

    // 5. Switch to Interactive tab
    click("demo_tab_Interactive")
    assertVisible("HTML with JavaScript interactions")

    // 6. Navigate back to Home screen
    click("back_button")

    // 7. Verify dismissed back to Home Screen
    assertVisible("CMP WebView Demo")
  }

  /**
   * Test 2: URL Loading Demo — verifies selector options and WebView container mount.
   */
  @Test
  fun testUrlLoadingWorkflow() = e2eTest {
    assertVisible("CMP WebView Demo")
    //assertVisible("demo_card_URL Loading")

    // Navigate to URL Loading Demo
    click("demo_card_URL Loading")

    // Click Example button and verify page loaded
    click("btn_example")
    waitForLoaded()

    // Click HTML5 Test button and verify page loaded
    click("btn_html5test")
    waitForLoaded()

    // Click YouTube button and verify page loaded
    click("btn_youtube")
    waitForLoaded()

    // Navigate back to Home screen
    click("back_button")
    assertVisible("CMP WebView Demo")
  }

  /**
   * Test 3: Content Switching Demo — verifies mode toggling between URL and HTML.
   */
  @Test
  fun testContentSwitchingWorkflow() = e2eTest {
    assertVisible("CMP WebView Demo")

    // Scroll to and navigate to Content Switching Demo
    scrollUntilVisible(containerTag = "home_list", targetTag = "demo_card_Content Switching")
    click("demo_card_Content Switching")

    // Verify title and mode switch options
    assertVisible("Content Switching Demo")
    assertVisible("Current Mode:")

    // Navigate back to Home screen
    click("back_button")
    assertVisible("CMP WebView Demo")
  }

  /**
   * Test 4: Multiple WebViews Demo — verifies concurrent WebView containers.
   */
  @Test
  fun testMultipleWebViewsWorkflow() = e2eTest {
    assertVisible("CMP WebView Demo")

    // Scroll to and navigate to Multiple WebViews Demo
    scrollUntilVisible(containerTag = "home_list", targetTag = "demo_card_Multiple WebViews")
    click("demo_card_Multiple WebViews")

    // Verify title and layout mode selector
    assertVisible("Multiple WebViews Demo")
    assertVisible("Layout Mode:")

    // Navigate back to Home screen
    click("back_button")
    assertVisible("CMP WebView Demo")
  }
}
