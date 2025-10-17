# Module cmp-webview

A WebView component for Compose Multiplatform that works across Android, iOS, Desktop (JVM), and Web (WASM) platforms.

## Features

- Load remote URLs or HTML content directly
- JavaScript-to-Compose communication via message bridge
- Loading state callbacks (started, finished, error)
- Custom HTTP headers for URL requests
- Consistent API across all platforms

## Installation

Add the dependency to your `commonMain` source set:

```kotlin
commonMain.dependencies {
  implementation("io.github.aryapreetam:cmp-webview:VERSION")
}
```

Replace `VERSION` with the latest release version.

**That's it!** No additional Gradle plugins or build configuration required.

## Platform Support

| Platform | Status | Implementation |
|----------|--------|----------------|
| Android (API 21+) | ✅ Supported | WebView via [compose-webview](https://github.com/KevinnZou/compose-webview) |
| iOS | ✅ Supported | WKWebView (native) |
| Desktop (JVM) | ✅ Supported | KCEF (Chromium Embedded Framework) |
| Web (WASM) | ✅ Supported | WebElementView with iframe (requires CMP 1.9.0+) |

### Platform-Specific Notes

#### Android

The Android implementation uses the [compose-webview library by KevinnZou](https://github.com/KevinnZou/compose-webview), which provides a Compose-friendly wrapper around Android's WebView.

**Required Configuration:**

Your app needs the `INTERNET` permission to load remote content. Add this to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET"/>
```

Without this permission, the WebView will not be able to fetch network resources.

**No other configuration needed** - the library handles everything else automatically.

#### Desktop (JVM)

The desktop implementation uses [KCEF by DatL4g](https://github.com/DatL4g/KCEF), which wraps the Chromium Embedded Framework.

**No manual setup required** - KCEF is automatically initialized by the library. On the first launch:
- Chromium will be downloaded and set up (~100MB)
- This happens automatically in the background
- A progress indicator is shown during download
- Subsequent launches are fast as Chromium is cached locally

**No JVM arguments, native libraries, or build configuration needed** - everything is handled automatically.

#### Web (WASM)

The WASM implementation uses `androidx.compose.ui.viewinterop.WebElementView` with HTML iframes, which was introduced in Compose Multiplatform 1.9.0.

**Requirements:**
- Compose Multiplatform 1.9.0 or higher
- No additional configuration needed

If you need to use this library with CMP < 1.9.0, please request this feature.

**Known Limitation:** Some websites (like GitHub, Google services, and banking sites) cannot be embedded in iframes due to Content Security Policy (CSP) restrictions. This is a browser security feature, not a library bug. When such sites are loaded, the browser will show its default error message (e.g., "refused to connect"). For more details about CSP limitations, see [`docs/wasm-iframe-csp-limitations.md`](https://github.com/aryapreetam/cmp-webview/blob/main/docs/wasm-iframe-csp-limitations.md).

#### iOS

The iOS implementation uses WKWebView, Apple's native web rendering engine.

**No configuration required** - everything works out of the box.

## Quick Start

### Loading a URL

```kotlin
import io.github.aryapreetam.cmpwebview.WebView
import androidx.compose.ui.Modifier

WebView(
  url = "https://example.com",
  modifier = Modifier.fillMaxSize()
)
```

### Loading HTML Content

```kotlin
WebView(
  htmlContent = """
    <html>
      <body>
        <h1>Hello from WebView</h1>
      </body>
    </html>
  """.trimIndent(),
  modifier = Modifier.fillMaxSize()
)
```

### With Callbacks

```kotlin
WebView(
  url = "https://example.com",
  onLoadStarted = { 
    println("Loading started") 
  },
  onLoadFinished = { 
    println("Loading finished") 
  },
  onLoadError = { error -> 
    println("Error: $error") 
  }
)
```

## JavaScript Bridge

The library provides one-way communication from JavaScript to Compose. Web pages can send messages to your Compose code using the `ComposeWebViewBridge` API.

### Sending Messages from JavaScript

In your web page JavaScript:

```javascript
// Wait for the bridge to be ready
document.addEventListener('ComposeWebViewBridgeReady', function() {
  // Send a message to Compose
  window.ComposeWebViewBridge.postMessage('Hello from JavaScript!');
});
```

### Receiving Messages in Compose

```kotlin
WebView(
  url = "https://example.com",
  onScriptResult = { message ->
    println("Received from JavaScript: $message")
  }
)
```

The bridge automatically injects the necessary JavaScript into loaded pages, so you don't need to include any additional libraries in your web content.

## Custom Headers

Load URLs with custom HTTP headers (useful for authentication):

```kotlin
WebView(
  url = "https://api.example.com",
  headers = mapOf(
    "Authorization" to "Bearer your-token-here",
    "Custom-Header" to "value"
  )
)
```

## Loading HTML Files from Resources

You can load HTML files bundled with your app using Compose Resources:

```kotlin
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MyScreen() {
  val htmlContent = Res.readBytes("files/my-page.html")
    .decodeToString()
  
  WebView(htmlContent = htmlContent)
}
```

Place your HTML files in `commonMain/composeResources/files/` and access them using the generated `Res` object.

## License

This library is open source. See the LICENSE file for details.
