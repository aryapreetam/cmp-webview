# Testing Guide

This guide walks you through testing WebView integrations across different platforms and understanding the interop testing limitations.

---

## 🧪 Testing Guide

### Interop testing limitations (important)

`WebView` is an **interop** component on several targets:

- **Android**: `AndroidView` ✅ testable via Android **instrumented** Compose UI tests
- **iOS**: `UIKitView` ⚠️ `runComposeUiTest` currently fails for interop views (`LocalInteropContainer not provided`)
- **Web/WASM**: `WebElementView` ⚠️ `runComposeUiTest` currently fails for interop views (`LocalInteropContainer not provided`)
- **Desktop/JVM**: `SwingPanel` ⚠️ `runComposeUiTest` does not provide `LocalInteropContainer` for `SwingPanel`

Practical strategy:

- Prefer **Android instrumented tests** for true end-to-end WebView behavior.
- On **Desktop/JVM**, use a Swing/ComposeWindow/ComposePanel-based integration harness (see the sample test at `sample/composeApp/src/jvmTest/.../WebViewBridgeIntegrationJvmTest.kt`).
- For **iOS/WASM**, rely on manual QA for the interop surface + common/unit tests for shared bridge logic.

### UI Testing with Test Tags

Use the `testTag` parameter to identify WebViews in tests:

> Note: The snippet below assumes an Android-style Compose UI test rule. For Desktop interop views, use a
> ComposePanel/ComposeWindow-based harness; for iOS/WASM, avoid `runComposeUiTest` for interop.

```kotlin
// In your composable
@Composable
fun MyScreen() {
  WebView(
    url = "https://example.com",
    testTag = "main-webview"
  )
}

// In your test
@Test
fun testWebViewLoads() {
  composeTestRule.setContent {
    MyScreen()
  }

  // Find WebView by test tag
  composeTestRule
    .onNodeWithTag("main-webview")
    .assertExists()
}
```

### Testing Loading States with Semantics

The WebView exposes loading state through semantics:

```kotlin
@Test
fun testLoadingState() {
  composeTestRule.setContent {
    WebView(
      url = "https://example.com",
      testTag = "test-webview"
    )
  }

  // Verify loading state is announced
  composeTestRule
    .onNodeWithTag("test-webview")
    .assertExists()
    .assert(hasStateDescription("Loading"))

  // Wait for loaded state
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule
      .onNodeWithTag("test-webview")
      .fetchSemanticsNode()
      .config[SemanticsProperties.StateDescription] == "Loaded"
  }
}
```

### Testing Bridge Communication

```kotlin
@Test
fun testBridgeMessage() {
  var receivedMessage: String? = null

  composeTestRule.setContent {
    WebView(
      htmlContent = """
                <html><body><script>
                    window.addEventListener('ComposeWebViewBridgeReady', function() {
                        ComposeWebViewBridge.postMessage('test-message');
                    });
                </script></body></html>
            """.trimIndent(),
      onScriptResult = { message ->
        receivedMessage = message
      }
    )
  }

  // Wait for bridge message
  composeTestRule.waitUntil(timeoutMillis = 3000) {
    receivedMessage == "test-message"
  }

  assertEquals("test-message", receivedMessage)
}
```
