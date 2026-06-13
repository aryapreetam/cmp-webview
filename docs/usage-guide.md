# Usage & Security Guide

This guide describes how to use various features of the `cmp-webview` library and implement security best practices.

---

## 📖 Usage Guide

### Loading URLs with Custom Headers

**Note:** Custom headers are currently supported on **Android only**. Other targets will ignore headers or do not support attaching headers to a top-level navigation.

```kotlin
WebView(
  url = "https://api.example.com/data",
  headers = mapOf(
    "Authorization" to "Bearer YOUR_TOKEN",
    "User-Agent" to "MyApp/1.0"
  ),
  onLoadFinished = { println("Authenticated content loaded") }
)
```

### Loading HTML with Base URL

Use `baseUrl` to resolve relative links in your HTML:

```kotlin
val html = """
    <html>
        <body>
            <img src="images/logo.png" />
            <a href="page2.html">Next Page</a>
        </body>
    </html>
""".trimIndent()

WebView(
  htmlContent = html,
  baseUrl = "https://example.com/",  // Resolves to example.com/images/logo.png
)
```

### Error Handling

```kotlin
var errorMessage by remember { mutableStateOf<String?>(null) }

if (errorMessage != null) {
  Text("Error: $errorMessage")
  Button(onClick = { errorMessage = null }) {
    Text("Retry")
  }
} else {
  WebView(
    url = "https://example.com",
    onLoadError = { error ->
      errorMessage = error
    }
  )
}
```

---

## 🔒 Security Best Practices

### URL Validation

The library automatically rejects dangerous URL schemes:

- ❌ `javascript:` URLs (XSS risk)
- ❌ `vbscript:` URLs (XSS risk)
- ❌ `file:` URLs (local file access)
- ✅ `https:` URLs (recommended)
- ✅ `http:` URLs (use with caution)

**Best Practices:**

1. **Use HTTPS only in production** to prevent man-in-the-middle attacks
2. **Validate URLs from user input** before passing to WebView
3. **Use URL allowlists** for untrusted sources
4. **Sanitize HTML content** from untrusted sources to prevent XSS

```kotlin
// Example: URL validation
fun loadUserUrl(userInput: String) {
  val url = userInput.trim()

  // Validate scheme
  if (!url.startsWith("https://")) {
    showError("Only HTTPS URLs are allowed")
    return
  }

  // Validate against allowlist
  val allowedDomains = listOf("example.com", "trusted-site.com")
  if (!allowedDomains.any { url.contains(it) }) {
    showError("Domain not allowed")
    return
  }

  // Safe to load
  WebView(url = url)
}
```

### Bridge Message Security

Always validate and sanitize messages from JavaScript:

```kotlin
WebView(
  url = "https://example.com",
  onScriptResult = { message ->
    try {
      // Validate message format
      require(message.length < 10_000) { "Message too large" }

      // Parse and validate JSON
      val data = Json.decodeFromString<MyData>(message)
      require(data.userId.isNotBlank()) { "Invalid user ID" }

      // Process validated data
      handleValidatedData(data)
    } catch (e: Exception) {
      Log.w("WebView", "Invalid bridge message: ${e.message}")
    }
  }
)
```

### Content Security Policy (CSP)

For HTML content, include CSP headers to restrict resource loading:

```kotlin
val secureHtml = """
    <html>
        <head>
            <meta http-equiv="Content-Security-Policy" 
                  content="default-src 'self' https://trusted-cdn.com; script-src 'self'">
        </head>
        <body>...</body>
    </html>
""".trimIndent()

WebView(htmlContent = secureHtml)
```
