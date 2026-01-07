# Module cmp-webview

A WebView component for Compose Multiplatform that works across Android, iOS, Desktop (JVM), and Web (WASM) platforms.

## Features

- Load remote URLs or HTML content directly
- JavaScript → Compose communication via message bridge (`onScriptResult`)
- Compose → JavaScript calls via `WebViewController.evaluateJavaScript` (best-effort; platform-dependent)
- Loading state callbacks (started, finished, error)
- Custom HTTP headers for URL requests (Android only)
- Single API across all platforms (some capabilities are platform-dependent)

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

## Capability matrix (quick truth table)

| Capability | Android | iOS | Desktop (JVM) | Web (WASM) |
|---|---:|---:|---:|---:|
| Load remote `url` | ✅ | ✅ | ✅ | ✅ *(iframe; subject to CSP/X-Frame-Options)* |
| Load `htmlContent` | ✅ | ✅ | ✅ | ✅ |
| Custom request headers (`headers`) | ✅ | ❌ | ❌ | ❌ |
| JS → Compose (`onScriptResult`) with `htmlContent` | ✅ | ✅ | ✅ | ✅ |
| JS → Compose (`onScriptResult`) with remote `url` | ✅ *(bridge injected)* | ✅ *(bridge injected)* | ✅ *(bridge injected)* | ⚠️ *best-effort (no cross-origin injection)* |
| Compose → JS (`WebViewController.evaluateJavaScript`) | ✅ | ✅ | ✅ *(executes; no return values yet)* | ✅ *(same-origin / `htmlContent` only)* |
| `evaluateJavaScript` **return values** | ✅ | ✅ | ❌ *(returns `Unsupported`)* | ✅ *(same-origin / `htmlContent` only)* |

Notes:
- On **Web/WASM**, browsers prevent injecting scripts into **cross-origin** iframes.
- On native targets, bridge injection can still be affected by page security policies (e.g., strict CSP). Treat messaging as best-effort for arbitrary third-party pages.

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

The library supports:

- **JavaScript → Compose** via `onScriptResult` and the injected `ComposeWebViewBridge` API
- **Compose → JavaScript** via `WebViewController.evaluateJavaScript` (optional)

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

On Android/iOS/Desktop, the library injects the `ComposeWebViewBridge` script into loaded pages.

On Web (WASM), the bridge is injected only when using `htmlContent` (via `iframe.srcdoc`). Browsers do not allow injecting scripts into cross-origin iframes, so JavaScript messaging for arbitrary remote URLs on WASM is **best-effort** and typically requires pages you control.

### Calling JavaScript from Compose (optional)

To call JavaScript, create a controller and pass it into `WebView`:

```kotlin
@Composable
fun ComposeToJsExample() {
  val controller = rememberWebViewController()
  val scope = rememberCoroutineScope()

  Column {
    Button(onClick = {
      scope.launch {
        controller.evaluateJavaScript("document.body.style.background = 'tomato';")
      }
    }) {
      Text("Run JS")
    }

    WebView(
      htmlContent = "<html><body>...</body></html>",
      controller = controller,
    )
  }
}
```

Notes:
- On **Desktop/JVM**, scripts execute best-effort, but return values are not available yet.
- On **Web/WASM**, calling JS is only supported for `htmlContent` or same-origin content.

## Custom Headers

**Note:** Custom headers are currently supported on **Android only**.

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
