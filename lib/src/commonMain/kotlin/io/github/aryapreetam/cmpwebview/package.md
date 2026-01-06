Package io.github.aryapreetam.cmpwebview

WebView components for loading web content in Compose Multiplatform applications.

## Overview

This package provides two primary composable functions for displaying web content:

- `WebView(url: String, ...)` - Load content from a remote URL
- `WebView(htmlContent: String, ...)` - Display HTML content directly

Both functions share the same API across Android, iOS, Desktop, and Web platforms, but some capabilities are platform-dependent.

## Capability matrix (quick truth table)

| Capability | Android | iOS | Desktop (JVM) | Web (WASM) |
|---|---:|---:|---:|---:|
| Load remote `url` | ✅ | ✅ | ✅ | ✅ *(iframe; subject to CSP/X-Frame-Options)* |
| Load `htmlContent` | ✅ | ✅ | ✅ | ✅ |
| Custom request headers (`headers`) | ✅ | ❌ | ❌ | ❌ |
| JS → Compose (`onScriptResult`) with `htmlContent` | ✅ | ✅ | ✅ | ✅ |
| JS → Compose (`onScriptResult`) with remote `url` | ✅ *(bridge injected)* | ✅ *(bridge injected)* | ✅ *(bridge injected)* | ⚠️ *best-effort (no cross-origin injection)* |

Notes:
- On **Web/WASM**, browsers prevent injecting scripts into **cross-origin** iframes.
- Custom headers are currently supported on **Android only**.

## Basic Usage

### Loading a Remote URL

```kotlin
WebView(
  url = "https://example.com",
  modifier = Modifier.fillMaxSize()
)
```

### Displaying HTML Content

```kotlin
WebView(
  htmlContent = """
    <html>
      <head><title>My Page</title></head>
      <body>
        <h1>Welcome</h1>
        <p>This is embedded HTML content.</p>
      </body>
    </html>
  """.trimIndent()
)
```

### Loading HTML from Resource Files

Place your HTML files in `commonMain/composeResources/files/` and load them using Compose Resources:

```kotlin
@OptIn(ExperimentalResourceApi::class)
@Composable
fun BridgeDemo() {
  val htmlContent = remember {
    Res.readBytes("files/test-bridge.html").decodeToString()
  }
  
  WebView(
    htmlContent = htmlContent,
    onScriptResult = { message ->
      println("Message from JS: $message")
    }
  )
}
```

**File structure:**
```
commonMain/
  composeResources/
    files/
      test-bridge.html
      my-form.html
```

## Monitoring Load States

Track when pages start loading, finish loading, or encounter errors:

```kotlin
var isLoading by remember { mutableStateOf(false) }

WebView(
  url = "https://example.com",
  onLoadStarted = {
    isLoading = true
    println("Loading started")
  },
  onLoadFinished = {
    isLoading = false
    println("Page loaded successfully")
  },
  onLoadError = { error ->
    isLoading = false
    println("Failed to load: $error")
  }
)
```

## JavaScript Communication

JavaScript code in web pages can send messages to Compose using the `ComposeWebViewBridge` API.

### From JavaScript to Compose

**In your HTML/JavaScript:**
```html
<script>
document.addEventListener('ComposeWebViewBridgeReady', function() {
  // Bridge is ready, send a message
  window.ComposeWebViewBridge.postMessage('Hello from JavaScript!');
  
  // Send structured data as JSON
  const data = { type: 'user_action', value: 42 };
  window.ComposeWebViewBridge.postMessage(JSON.stringify(data));
});
</script>
```

**In your Compose code:**
```kotlin
WebView(
  htmlContent = myHtml,
  onScriptResult = { message ->
    // Handle simple string messages
    if (message == "Hello from JavaScript!") {
      println("Greeting received!")
    }
    
    // Parse JSON messages
    try {
      val json = Json.parseToJsonElement(message).jsonObject
      val type = json["type"]?.jsonPrimitive?.content
      if (type == "user_action") {
        val value = json["value"]?.jsonPrimitive?.int
        println("User action with value: $value")
      }
    } catch (e: Exception) {
      println("Received: $message")
    }
  }
)
```

### Bridge Communication Example

Complete example with form submission:

```kotlin
val formHtml = """
  <!DOCTYPE html>
  <html>
  <head>
    <style>
      body { font-family: sans-serif; padding: 20px; }
      input, button { margin: 10px 0; padding: 8px; }
    </style>
  </head>
  <body>
    <h2>Contact Form</h2>
    <input type="text" id="name" placeholder="Your name">
    <input type="email" id="email" placeholder="Your email">
    <button onclick="submitForm()">Submit</button>
    
    <script>
    document.addEventListener('ComposeWebViewBridgeReady', function() {
      console.log('Bridge is ready!');
    });
    
    function submitForm() {
      const name = document.getElementById('name').value;
      const email = document.getElementById('email').value;
      
      const data = {
        type: 'form_submit',
        name: name,
        email: email
      };
      
      window.ComposeWebViewBridge.postMessage(JSON.stringify(data));
    }
    </script>
  </body>
  </html>
""".trimIndent()

@Composable
fun FormDemo() {
  WebView(
    htmlContent = formHtml,
    onScriptResult = { message ->
      val data = Json.parseToJsonElement(message).jsonObject
      if (data["type"]?.jsonPrimitive?.content == "form_submit") {
        val name = data["name"]?.jsonPrimitive?.content
        val email = data["email"]?.jsonPrimitive?.content
        println("Form submitted: name=$name, email=$email")
      }
    }
  )
}
```

## Custom Headers

Load URLs with custom HTTP headers for authentication or other purposes:

```kotlin
WebView(
  url = "https://api.example.com/protected",
  headers = mapOf(
    "Authorization" to "Bearer ${getAccessToken()}",
    "X-API-Key" to "your-api-key",
    "Accept" to "text/html"
  ),
  onLoadError = { error ->
    println("Authentication may have failed: $error")
  }
)
```

**Note:** Custom headers only work with URL loading, not with HTML content.

## Testing

Use Compose test tags to identify WebView components in UI tests:

```kotlin
// In your composable
WebView(
  url = "https://example.com",
  modifier = Modifier.testTag("main-webview")
)

// In your test
composeTestRule.onNodeWithTag("main-webview").assertExists()
```

## Security Considerations

### URL Validation

The library automatically rejects URLs with dangerous schemes:
- `javascript:` URLs (XSS risk)
- `vbscript:` URLs (XSS risk)  
- `file:` URLs (local file access)

Always validate and sanitize URLs from user input before passing them to `WebView()`.

### HTML Content

When displaying HTML content from untrusted sources:
- Sanitize the HTML to prevent XSS attacks
- Avoid including `<script>` tags from untrusted sources
- Be cautious with the `baseUrl` parameter as it affects how relative URLs are resolved

### HTTPS Recommendations

For production applications, use HTTPS URLs to ensure:
- Encrypted communication
- Content integrity
- Protection against man-in-the-middle attacks

## Accessibility

The WebView component includes semantic information for screen readers:
- Content description: "Web content display"
- State description: Updates with loading state (Idle, Loading, Loaded, Error)

This helps users of assistive technologies understand what the component is and its current state.
