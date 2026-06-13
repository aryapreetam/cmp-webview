<h1 align="center">CMP WebView - Compose Multiplatform WebView Library</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Compose%20Multiplatform-v1.10.1-4285F4?logo=jetpackcompose" alt="Compose Multiplatform Version">
  <img src="https://img.shields.io/badge/Kotlin-v2.3.21-blueviolet?logo=kotlin" alt="Kotlin Version">
</p>

<p align="center">
  <a href="https://github.com/aryapreetam/cmp-webview/actions/workflows/release.yml">
    <img src="https://github.com/aryapreetam/cmp-webview/actions/workflows/release.yml/badge.svg" alt="Release workflow status">
  </a>
  <a href="https://mvnrepository.com/artifact/io.github.aryapreetam/cmp-webview">
    <img src="https://img.shields.io/maven-central/v/io.github.aryapreetam/cmp-webview?label=Maven%20Central&color=blue" alt="Maven Central Version">
  </a>
  <a href="https://github.com/aryapreetam/cmp-webview/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-green.svg" alt="MIT License">
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/iOS-000000?logo=apple&logoColor=white" alt="iOS">
  <img src="https://img.shields.io/badge/Desktop/JVM-0078D6?logo=desktop&logoColor=white" alt="Desktop">
  <img src="https://img.shields.io/badge/Web(wasm)-4285F4?logo=google-chrome&logoColor=white" alt="Web">
</p>

A WebView library for Compose Multiplatform that enables you to display web content and HTML across
Android, iOS, Desktop (JVM), and Web (WASM) platforms with a unified API.

**Key Features:**

- 🌐 Load remote URLs (custom headers: Android only)
- 📄 Display HTML content directly
- 🔗 JavaScript → Compose messaging (via `onScriptResult`)
- 🔁 Compose → JavaScript calls (via `WebViewController.evaluateJavaScript`)
- 🔒 Built-in security protections against dangerous URL schemes
- ♿ Accessibility support with screen reader compatibility
- 🧪 Testing support with test tags and semantics
- 🎯 Single API across all platforms (some capabilities are platform-dependent)

## ✅ Platform capability matrix

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
- On **Web/WASM**, browsers prevent injecting scripts into **cross-origin** iframes. For reliable messaging on WASM, prefer `htmlContent` or same-origin pages you control.
- On native targets, bridge injection can still be affected by page security policies (e.g., strict CSP). Treat messaging as best-effort for arbitrary third-party pages.
- On **Desktop/JVM**, JavaScript execution is best-effort, but `evaluateJavaScript` currently does **not** surface return values (it returns `WebViewJsResult.Unsupported(...)`).

Wasm Demo:
[![Try Live Demo](https://img.shields.io/badge/Try%20Live%20Demo-Web%20wasmJs-blueviolet?style=for-the-badge)](https://aryapreetam.github.io/cmp-webview/demo/)

API Docs:
[![Library API Docs](https://img.shields.io/badge/API%20Reference-Library%20Docs-green?style=for-the-badge)](https://aryapreetam.github.io/cmp-webview/api/)

---

## 📦 Installation

Add the dependency to your Compose Multiplatform project:

**Option A — Version catalog (recommended)**

1) In `gradle/libs.versions.toml`:

```toml
[versions]
cmpWebview = "0.0.1"

[libraries]
cmp-webview = { module = "io.github.aryapreetam:cmp-webview", version.ref = "cmpWebview" }
```

2) In your module's `build.gradle.kts`:

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

**Option B — Direct dependency**

```kotlin
kotlin {
  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation("io.github.aryapreetam:cmp-webview:0.0.1")
      }
    }
  }
}
```

---

## 🚀 Quick Start

### Load a Remote URL

```kotlin
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

### Display HTML Content

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

JavaScript code in your web content can communicate with your Compose code:

**In your HTML/JavaScript:**

```javascript
// Listen for bridge ready event
window.addEventListener('ComposeWebViewBridgeReady', function () {
  // Send message to Compose
  ComposeWebViewBridge.postMessage('Hello from JavaScript!');
});

// Example: Send button click data
document.getElementById('myButton').addEventListener('click', function () {
  ComposeWebViewBridge.postMessage(JSON.stringify({
    action: 'buttonClick',
    data: 'some value'
  }));
});
```

**In your Compose code:**

```kotlin
@Composable
fun BridgeExample() {
  WebView(
    url = "https://example.com",
    onScriptResult = { message ->
      println("Received from JavaScript: $message")
      // Parse JSON if needed
      val data = Json.decodeFromString<MyData>(message)
      // Handle the message
    }
  )
}
```

### Compose → JavaScript (optional)

If you need to call JavaScript from Compose, pass a `WebViewController`.

```kotlin
@Composable
fun ComposeToJsExample() {
  val controller = rememberWebViewController()
  val scope = rememberCoroutineScope()

  Column {
    Button(onClick = {
      scope.launch {
        controller.evaluateJavaScript(
          "document.body.style.background = 'tomato';" +
            "window.ComposeWebViewBridge?.postMessage('ack');"
        )
      }
    }) {
      Text("Run JS")
    }

    WebView(
      htmlContent = "<html><body>...</body></html>",
      controller = controller,
      onScriptResult = { msg -> println("JS→Compose: $msg") },
    )
  }
}
```

Notes:
- On **Desktop/JVM**, the script runs best-effort, but return values are not available yet.
- On **Web/WASM**, calling JS only works reliably for `htmlContent` or same-origin content.

---

## 📚 Documentation

Detailed documentation is available in the `docs` directory:

* **[Usage & Security Guide](docs/usage-guide.md)**: Custom headers, base URL, error handling, URL validation, bridge message security, and Content Security Policy (CSP).
* **[Testing Guide](docs/testing-guide.md)**: Interop testing limitations, UI testing with test tags, semantics for loading states, and testing bridge communication.
* **[Accessibility Guide](docs/accessibility.md)**: Automatic semantics, screen reader announcements, and best practices.
* **[Troubleshooting Guide](docs/troubleshooting.md)**: Solutions for bridge messaging, load errors, blank displays, IDE unresponsiveness, memory leaks, and Desktop session limitations.
* **[Performance Considerations](docs/performance-considerations.md)**: Memory management, caching behavior, and optimization best practices.

---

## 🔧 Platform-Specific Notes

### Android

- Uses native Android WebView
- JavaScript is enabled by default
- Supports all standard web features

### iOS

- Uses WKWebView
- Supports modern web standards
- Bridge communication via WKScriptMessageHandler

### Desktop (JVM)

- Uses Wry (native OS web engine via ComposeNativeWebview)
- No Chromium bundling — uses the platform's native web engine
- Bridge communication via JS polyfill (`window.javaBridge` → `window.kmpJsBridge`)

### Web (WASM)

- Uses HTML iframe
- **Cross-origin limitations:** Bridge only works for same-origin content
- For cross-origin URLs, the iframe cannot inject bridge scripts due to browser security

---

## 📦 Download Sample App Executables

Download and try the sample app on your platform without building from source:

| Platform | Download Link |
|----------|--------------|
| 🍏 **macOS (Intel)** | [![Download](https://img.shields.io/badge/Download--Mac--x64--DMG-blue?style=for-the-badge)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-desktop-x64.dmg) |
| 🍎 **macOS (Apple Silicon)** | [![Download](https://img.shields.io/badge/Download--Mac--arm64--DMG-blue?style=for-the-badge)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-desktop-arm64.dmg) |
| 🪟 **Windows** | [![Download](https://img.shields.io/badge/Download--Windows--MSI-0078D4?style=for-the-badge&logo=windows)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-windows.msi) |
| 🐧 **Linux** | [![Download](https://img.shields.io/badge/Download--Linux--DEB-E95420?style=for-the-badge&logo=ubuntu)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-linux.deb) |
| 🤖 **Android** | [![Download](https://img.shields.io/badge/Download--Android--APK-green?style=for-the-badge&logo=android)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-android-unsigned.apk) |
| 🌐 **Web (Wasm)** | [![Download](https://img.shields.io/badge/Download--Web--Wasm--zip-purple?style=for-the-badge)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-wasm.zip) |
|  **iOS Simulator** | [![Download](https://img.shields.io/badge/Download--iOS--Simulator--zip-orange?style=for-the-badge&logo=apple)](https://github.com/aryapreetam/cmp-webview/releases/latest/download/sample-app-ios-simulator.zip) |

### 🚀 How to Run the Sample App

For instructions on how to install and run the pre-built executables or build and run the sample application from source code, please refer to the **[How to Run the Sample App](docs/run-sample-app.md)** guide.

---



## License

MIT License © 2025 aryapreetam and contributors. See [LICENSE](./LICENSE) for details.

---

## 🙏 Acknowledgments



- [ComposeNativeWebview](https://github.com/kdroidFilter/ComposeNativeWebview) - Native Webview for Compose Multiplatform (Desktop/JVM).
- [compose-webview-multiplatform](https://github.com/KevinnZou/compose-webview-multiplatform) - WebView for JetBrains Compose Multiplatform 
---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed contribution guidelines.

