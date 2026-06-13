# Accessibility Guide

This guide details how the `cmp-webview` library integrates accessibility semantics to support screen readers (such as Android TalkBack and iOS VoiceOver).

---

## ♿ Accessibility

The WebView library provides built-in accessibility support for screen readers like TalkBack (Android) and VoiceOver (iOS).

### Automatic Semantics

Every WebView automatically provides:

- **Content Description:** "Web content display"
- **State Description:** Current loading state ("Idle", "Loading", "Loaded", "Error")

### Screen Reader Announcements

```kotlin
WebView(
  url = "https://example.com",
  onLoadStarted = {
    // Screen reader announces: "Web content display, Loading"
  },
  onLoadFinished = {
    // Screen reader announces: "Web content display, Loaded"
  },
  onLoadError = { error ->
    // Screen reader announces: "Web content display, Error"
  }
)
```

### Best Practices

1. **Provide meaningful test tags** for navigation:
   ```kotlin
   WebView(url = "...", testTag = "article-webview")
   ```

2. **Use descriptive error messages** for screen readers:
   ```kotlin
   WebView(
       url = "...",
       onLoadError = { error ->
           announceForAccessibility("Failed to load page: $error")
       }
   )
   ```
