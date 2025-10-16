# WASM Target: iframe CSP Limitations

**Last Updated:** October 17, 2025

## Overview

On the **WASM (Web)** target, the WebView is implemented using standard HTML `<iframe>` elements. While this works perfectly for most websites, certain sites—including **GitHub.com**—cannot be embedded due to Content Security Policy (CSP) restrictions they enforce.

## The Problem

Many security-conscious websites set a CSP header that prevents their content from being embedded in iframes. The most common restriction is:

```
Content-Security-Policy: frame-ancestors 'none'
```

When you attempt to load such sites in the WebView on WASM, you'll encounter two types of errors:

1. **Visible error in the WebView/iframe**: 
   ```
   github.com refused to connect
   ```
   or
   ```
   [domain] refused to connect
   ```

2. **Browser console error** (for developers):
   ```
   Refused to frame 'https://github.com/' because an ancestor violates the 
   following Content Security Policy directive: "frame-ancestors 'none'".
   ```

The "refused to connect" message is what end-users see in the iframe when CSP blocks the embedding. This library automatically detects these situations and replaces the error with a user-friendly explanation.

### Why Do Sites Block Embedding?

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
| **Desktop (JVM)** | JCEF/CEF (native) | ✅ Yes - native WebView ignores CSP for embedding |
| **WASM (Web)** | `<iframe>` (browser) | ❌ No - browser enforces CSP strictly |

**Key Insight:** This is not a bug in the library—it's a fundamental limitation of how browsers handle iframes. There is **no client-side JavaScript workaround** to bypass CSP `frame-ancestors` restrictions in a browser.

## How This Library Handles Blocked Sites

When a site blocks embedding on the WASM target, the library automatically:

1. **Detects the CSP violation** via the browser's `securitypolicyviolation` event
2. **Shows a user-friendly fallback page** explaining why the site cannot be displayed
3. **Provides an "Open in New Tab" button** so users can still access the content

### Example Fallback UI

When you try to load `https://github.com`, users see:

```
🚫
Cannot Display This Site

This website restricts embedding in iframes for security reasons 
(CSP frame-ancestors policy).

https://github.com/

[Open in New Tab →]

Some sites like GitHub, Google, and banking sites prevent embedding 
to protect against clickjacking attacks. You can still visit the 
site by opening it in a new browser tab.
```

## Workaround: Using a Reverse Proxy (Advanced)

If you **must** display CSP-protected sites in the WebView on WASM, the only solution is to use a **reverse proxy** that you control. The proxy:

1. Fetches the target site's content
2. Strips or modifies the `Content-Security-Policy` and `X-Frame-Options` headers
3. Serves the content from your own origin

### ⚠️ Important Caveats

- **Legal/Terms of Service**: Proxying content may violate the site's terms of service. Always verify you have permission.
- **Authentication**: Users won't be logged in through the proxy (no cookies/sessions).
- **Limited Functionality**: Interactive features, scripts, and dynamic content may break.
- **Maintenance Overhead**: You need to host and maintain the proxy infrastructure.
- **Security Risk**: Your proxy becomes responsible for serving third-party content.

### Enabling Proxy Support

The library supports optional proxy configuration. To enable it:

1. **Set a global variable** in your host HTML page before the WASM app loads:

```html
<script>
  // Configure your proxy endpoint
  window.__CMPWV_PROXY = "https://your-proxy.example.com/proxy?url=";
</script>
```

2. **When a CSP block occurs**, the library will automatically retry loading via:
   ```
   https://your-proxy.example.com/proxy?url=<encoded-original-url>
   ```

3. **If the proxy succeeds**, the content displays. If it also fails, the fallback UI appears.

### Example Proxy Implementations

#### Option 1: Cloudflare Workers (Recommended)

```javascript
// worker.js
export default {
  async fetch(request) {
    const url = new URL(request.url);
    const target = url.searchParams.get("url");
    
    if (!target) {
      return new Response("Missing url parameter", { status: 400 });
    }

    try {
      const upstream = await fetch(target);
      
      // Clone response and strip CSP/frame headers
      const headers = new Headers(upstream.headers);
      headers.delete("content-security-policy");
      headers.delete("x-frame-options");
      
      return new Response(upstream.body, {
        status: upstream.status,
        headers,
      });
    } catch (error) {
      return new Response(`Proxy error: ${error.message}`, { status: 502 });
    }
  },
};
```

Deploy to Cloudflare Workers, then set:
```javascript
window.__CMPWV_PROXY = "https://your-worker.workers.dev/?url=";
```

#### Option 2: Node.js/Express (Local Development)

```javascript
import express from "express";
import fetch from "node-fetch";

const app = express();

app.get("/proxy", async (req, res) => {
  const target = req.query.url;
  
  if (!target) {
    return res.status(400).send("Missing url parameter");
  }

  try {
    const upstream = await fetch(target);
    
    res.status(upstream.status);
    upstream.headers.forEach((value, key) => {
      // Skip CSP and frame headers
      if (!["content-security-policy", "x-frame-options"].includes(key.toLowerCase())) {
        res.setHeader(key, value);
      }
    });
    
    upstream.body.pipe(res);
  } catch (error) {
    res.status(502).send(`Proxy error: ${error.message}`);
  }
});

app.listen(8787, () => console.log("Proxy running on :8787"));
```

Run locally, then set:
```javascript
window.__CMPWV_PROXY = "http://localhost:8787/proxy?url=";
```

## Recommended Approach

For most applications, we recommend:

1. **Detect and handle CSP blocks gracefully** (the library does this automatically)
2. **Show the fallback UI** and let users open blocked sites in a new tab
3. **Only use a proxy if absolutely necessary** and you understand the legal/technical implications

### Alternative Approaches

Instead of trying to embed CSP-protected sites, consider:

- **Deep linking**: Open blocked sites in the system browser
- **OAuth/API integration**: Use the site's API instead of embedding their UI
- **Screenshots/read-only views**: If you only need to display information, fetch via API and render yourself
- **Platform-specific behavior**: Use native WebView on mobile/desktop, limit WASM to embeddable sites

## Testing CSP Behavior

### Sites That **Allow** Embedding (Should Work)
- https://example.com
- https://wikipedia.org
- Most blogs and documentation sites
- Your own domains

### Sites That **Block** Embedding (Will Show Fallback)
- https://github.com
- https://google.com
- https://facebook.com
- https://twitter.com
- Most banking/financial sites

## Technical Details

### How Detection Works

1. **Browser CSP enforcement**: When the iframe tries to load a blocked site, the browser fires a `securitypolicyviolation` event
2. **Event listener**: The library listens for this event and checks if `violatedDirective` contains `"frame-ancestors"`
3. **Fallback activation**: If detected (and no proxy configured), the library replaces the iframe content with the fallback UI
4. **Load success**: If the site allows embedding, the `load` event fires and `onLoadFinished` callback is invoked

### Code Reference

The CSP detection logic is in:
```
lib/src/wasmJsMain/kotlin/io/github/aryapreetam/cmpwebview/WebView.wasmJs.kt
```

Key components:
- `SecurityPolicyViolationEvent` external class for type-safe CSP event handling
- `cspViolationHandler` that checks for `frame-ancestors` violations
- `handleBlocked()` function that tries proxy (if configured) or shows fallback
- `showBlockedSiteFallback()` that renders the user-friendly error page

## Summary

| Scenario | Behavior |
|----------|----------|
| **Embeddable site** (example.com) | ✅ Loads normally in iframe |
| **CSP-blocked site** without proxy (github.com) | 🚫 Shows fallback with "Open in New Tab" |
| **CSP-blocked site** with proxy configured | 🔄 Retries via proxy, then fallback if that fails |
| **Network error** | ⚠️ Shows error via `onLoadError` callback |

## See Also

- [Mozilla CSP Documentation](https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP)
- [frame-ancestors Directive](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/frame-ancestors)
- [Clickjacking Prevention](https://owasp.org/www-community/attacks/Clickjacking)

---

**Questions or issues?** Please open an issue on GitHub with details about the site you're trying to embed and your use case.
