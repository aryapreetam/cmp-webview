# WASM Target: iframe CSP Limitations

**Last Updated:** October 17, 2025

## Overview

On the **WASM (Web)** target, the WebView is implemented using `androidx.compose.ui.viewinterop.WebElementView` with standard HTML `<iframe>` elements. While this works for most websites, certain sites cannot be embedded due to Content Security Policy (CSP) restrictions they enforce.

## The Problem

Many security-conscious websites set a CSP header that prevents their content from being embedded in iframes. The most common restriction is:

```
Content-Security-Policy: frame-ancestors 'none'
```

When you attempt to load such sites in the WebView on WASM, you'll encounter:

1. **Browser console error** (for developers):
   ```
   Refused to frame 'https://github.com/' because an ancestor violates the 
   following Content Security Policy directive: "frame-ancestors 'none'".
   ```

2. **Visible error in the iframe** (what users see):
   ```
   github.com refused to connect
   ```
   or
   ```
   [domain] refused to connect
   ```

## Why Do Sites Block Embedding?

The `frame-ancestors` directive is a security feature designed to prevent **clickjacking attacks**, where malicious sites embed trusted pages in iframes and trick users into performing unintended actions.

Sites that commonly block iframe embedding include:
- **GitHub** (`frame-ancestors 'none'`)
- **Google services** (Gmail, Drive, etc.)
- **Banking and financial sites**
- **Social media platforms** (Facebook, Twitter/X)
- **Many enterprise/corporate sites**

## Platform Comparison

| Platform | Implementation | Can Embed CSP-Protected Sites? |
|----------|---------------|-------------------------------|
| **Android** | `WebView` (native) | ✅ Yes - native WebView ignores CSP for embedding |
| **iOS** | `WKWebView` (native) | ✅ Yes - native WebView ignores CSP for embedding |
| **Desktop (JVM)** | KCEF/CEF (native) | ✅ Yes - native WebView ignores CSP for embedding |
| **WASM (Web)** | `<iframe>` (browser) | ❌ No - browser enforces CSP strictly |

**Key Insight:** This is not a bug in the library—it's a fundamental limitation of how browsers handle iframes. There is **no client-side JavaScript workaround** to bypass CSP `frame-ancestors` restrictions in a browser.

## How This Library Handles Blocked Sites

The library does **not** implement special handling for CSP violations. When a site blocks embedding:

1. The browser displays its default error message in the iframe (e.g., "refused to connect")
2. An error message appears in the browser's developer console
3. The `onLoadError` callback may be triggered (platform-dependent)

This is the standard browser behavior, and the library lets the browser handle it naturally.

## Implementation Details

The WASM implementation (`lib/src/wasmJsMain/kotlin/io/github/aryapreetam/cmpwebview/WebView.wasmJs.kt`) uses:

```kotlin
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun WebViewImpl(...) {
  val iframe = remember {
    (document.createElement("iframe") as HTMLIFrameElement).apply {
      style.width = "100%"
      style.height = "100%"
      style.border = "none"
    }
  }
  
  // ... event listeners for load, error, etc.
  
  WebElementView(
    factory = { iframe },
    modifier = modifier,
    update = { /* content changes handled by LaunchedEffect */ }
  )
}
```

The implementation is straightforward: create an iframe, manage its content, and let the browser handle CSP enforcement.

## Workarounds

If you **must** display CSP-protected sites in the WebView on WASM, the only solution is to use a **reverse proxy** that you control. The proxy:

1. Fetches the target website on the server side
2. Strips or modifies CSP headers
3. Serves the modified content to your iframe

**Example proxy setup:**

```javascript
// Node.js/Express proxy example
app.get('/proxy', async (req, res) => {
  const url = req.query.url;
  const response = await fetch(url);
  const body = await response.text();
  
  // Set permissive headers
  res.setHeader('Content-Security-Policy', 'frame-ancestors *');
  res.send(body);
});
```

Then load via proxy:

```kotlin
WebView(
  url = "https://your-proxy.com/proxy?url=https://github.com"
)
```

**Warning:** Proxying content may violate the target site's terms of service and raises privacy/security concerns. Use responsibly and only for sites you own or have permission to proxy.

## Testing CSP Behavior

To test how your app handles CSP-blocked sites:

```kotlin
// This will fail on WASM (CSP blocked)
WebView(
  url = "https://github.com",
  onLoadError = { error ->
    println("Load error: $error")
    // Show user-friendly message
  }
)

// This works on all platforms (no CSP restrictions)
WebView(
  url = "https://example.com"
)
```

## Recommendations

1. **Test on WASM early** - Don't assume all URLs will work
2. **Use `onLoadError` callback** - Handle failures gracefully
3. **Provide fallback UI** - Show helpful messages when sites can't be embedded
4. **Consider alternatives** - For CSP-protected sites, consider:
   - Opening in a new browser tab
   - Using an API instead of embedding the full site
   - Creating your own web interface for the data

## Summary

- WASM uses standard browser iframes via `WebElementView`
- Browsers enforce CSP strictly for iframes (no workarounds)
- CSP-blocked sites show browser's default error message
- Native platforms (Android, iOS, Desktop) are not affected by iframe CSP restrictions
- Use a reverse proxy if you absolutely need to embed CSP-protected sites (with caution)

