# Troubleshooting Guide

Solutions to common issues you might encounter while using `cmp-webview`.

---

## 🐛 Troubleshooting

### Bridge Not Receiving Messages

**Problem:** JavaScript bridge messages aren't reaching Compose code.

**Solutions:**

1. **Wait for bridge ready:**
   ```javascript
   window.addEventListener('ComposeWebViewBridgeReady', function() {
       // Now safe to send messages
       ComposeWebViewBridge.postMessage('hello');
   });
   ```

2. **Check platform compatibility:**
    - Desktop: Ensure using `javaBridge`
    - Web/WASM: Only works for same-origin content

3. **Verify callback is set:**
   ```kotlin
   WebView(
       url = "...",
       onScriptResult = { message ->  // Must provide this callback
           println("Received: $message")
       }
   )
   ```

### Load Errors

**Problem:** WebView fails to load URL or shows error.

**Solutions:**

1. **Check internet connectivity**
2. **Verify URL is valid and accessible:**
   ```kotlin
   WebView(
       url = "https://example.com",  // Must be valid HTTPS/HTTP
       onLoadError = { error ->
           Log.e("WebView", "Load failed: $error")
       }
   )
   ```

3. **Check for blocked schemes:**
    - `javascript:`, `vbscript:`, `file:` are blocked for security

4. **Verify SSL certificate** (HTTPS only):
    - Self-signed certificates may be rejected on some platforms

### Content Not Displaying

**Problem:** WebView appears blank or doesn't show content.

**Solutions:**

1. **Check modifier size:**
   ```kotlin
   WebView(
       url = "...",
       modifier = Modifier.fillMaxSize()  // Or specific size
   )
   ```

2. **Verify content loaded:**
   ```kotlin
   WebView(
       url = "...",
       onLoadFinished = { 
           println("Content loaded successfully")
       }
   )
   ```

3. **Check HTML validity** (for HTML content):
   ```kotlin
   val html = "<html><body>Content</body></html>"  // Must be valid HTML
   WebView(htmlContent = html)
   ```

### Desktop: IDE Unresponsive After App Exit

**Problem:** After closing a Desktop app, the IDE or Gradle process may stay unresponsive.

**Solutions:**

1. **Use a compatible runtime:** ensure Gradle uses JetBrains Runtime (JBR).
2. **Let the runtime auto-clean up:** the library disposes native resources when the last desktop window closes.

### Memory Leaks

**Problem:** App memory grows over time with WebViews.

**Solutions:**

1. **WebView automatically cleans up** when disposed via DisposableEffect
2. **Desktop/JVM:** runtime cleanup is automatic on app exit
3. **Avoid keeping strong references** to WebView callbacks
4. **Test with memory profiler** to verify cleanup

### Desktop WebView Limitation

**Problem:** Desktop WebView can only be created once per application session.

**Solutions:**

1. **Reuse the same WebView instance** - change URL instead of recreating
2. **Use conditional composition:**
   ```kotlin
   var currentUrl by remember { mutableStateOf("https://example.com") }
   
   // Reuse same WebView, just change URL
   WebView(url = currentUrl)
   
   // Change URL instead of recreating
   Button(onClick = { currentUrl = "https://other-site.com" }) {
       Text("Load Other Site")
   }
   ```
