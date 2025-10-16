# Module CMP WebView

CMP WebView is a comprehensive WebView library for Compose Multiplatform that enables you to display web content and HTML across Android, iOS, Desktop (JVM), and Web (WASM) platforms with a unified API.

## Key Features

- 🌐 **Load remote URLs** with optional custom headers
- 📄 **Display HTML content** directly without a web server
- 🔗 **Bidirectional JavaScript bridge** for seamless communication between web and native code
- 🔒 **Built-in security protections** against dangerous URL schemes (javascript:, vbscript:, file:)
- ♿ **Accessibility support** with screen reader compatibility
- 🧪 **Testing support** via Modifier.testTag() and semantics
- 🎯 **Consistent API** across all platforms

## Installation

Add the dependency to your Compose Multiplatform project:

### Using Version Catalog (Recommended)

In `gradle/libs.versions.toml`:

```toml
[versions]
cmpWebview = "0.1.0"

[libraries]
cmp-webview = { module = "io.github.aryapreetam:cmp-webview", version.ref = "cmpWebview" }
```

In your module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.cmp.webview)
            }
        }
    }
}
```

### Direct Dependency

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.aryapreetam:cmp-webview:0.1.0")
            }
        }
    }
}
```

## Quick Start

### Loading a Remote URL

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aryapreetam.cmpwebview.WebView

@Composable
fun MyScreen() {
    WebView(
        url = "https://example.com",
        modifier = Modifier.fillMaxSize(),
        onLoadStarted = { println("Loading started") },
        onLoadFinished = { println("Loading finished") },
        onLoadError = { error -> println("Error: $error") }
    )
}
```

### Displaying HTML Content

```kotlin
@Composable
fun HtmlScreen() {
    val html = """
        <html>
            <body>
                <h1>Hello from HTML!</h1>
                <p>This is rendered locally.</p>
            </body>
        </html>
    """.trimIndent()
    
    WebView(
        htmlContent = html,
        modifier = Modifier.fillMaxSize()
    )
}
```

### JavaScript Bridge Communication

JavaScript in your web content can communicate with Compose:

**JavaScript:**
```javascript
// Listen for bridge ready event
window.addEventListener('ComposeWebViewBridgeReady', function() {
    // Send message to Compose
    ComposeWebViewBridge.postMessage('Hello from JavaScript!');
});
```

**Compose:**
```kotlin
@Composable
fun BridgeExample() {
    WebView(
        url = "https://example.com",
        onScriptResult = { message ->
            println("Received from JavaScript: $message")
        }
    )
}
```

## Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android  | ✅ Supported | Native Android WebView |
| iOS      | ✅ Supported | WKWebView |
| Desktop (JVM) | ✅ Supported | KCEF (Chromium) |
| Web (WASM) | ✅ Supported | HTML iframe |

### Desktop Requirements

The Desktop implementation uses KCEF (Kotlin Chromium Embedded Framework), which provides a modern Chromium-based browser engine.

**Important Notes:**
- On first run, KCEF will automatically download ~200MB of Chromium binaries (one-time only)
- The download is cached locally and reused for subsequent runs
- macOS users must add JVM flags to their application (see Installation section)

## Security

The library includes built-in security features:

- **URL validation**: Automatically rejects dangerous schemes (javascript:, vbscript:, file:)
- **HTTPS recommended**: Production apps should use HTTPS URLs
- **Input sanitization**: Always validate URLs from user input
- **Bridge message validation**: Validate and sanitize messages from JavaScript

## Testing

The library provides full testing support:

```kotlin
// In your composable
@Composable
fun MyScreen() {
    WebView(
        url = "https://example.com",
        modifier = Modifier.testTag("main-webview")
    )
}

// In your test
@Test
fun testWebViewLoads() {
    composeTestRule.setContent { MyScreen() }
    composeTestRule.onNodeWithTag("main-webview").assertExists()
}
```

## Accessibility

Every WebView automatically provides semantic information for screen readers:
- Content description: "Web content display"
- State description: Current loading state (Idle, Loading, Loaded, Error)

## Additional Resources

- [GitHub Repository](https://github.com/aryapreetam/cmp-webview)
- [Sample App](https://github.com/aryapreetam/cmp-webview/tree/main/sample)
- [Issue Tracker](https://github.com/aryapreetam/cmp-webview/issues)

## License

MIT License © 2025 aryapreetam and contributors
