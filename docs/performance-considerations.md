# Performance Considerations

Guidelines to help optimize memory usage, caching, and performance when using `cmp-webview` across platforms.

---

## 📊 Performance Considerations

### Memory Management

- WebView instances are **automatically cleaned up** when removed from composition
- Each platform implements proper disposal (removing bridge interfaces, clearing history, etc.)
- Callbacks are **not invoked after disposal** to prevent memory leaks

### Caching Behavior

- **Android:** Uses system WebView cache (configurable via WebSettings)
- **iOS:** Uses WKWebView cache (automatic)
- **Desktop:** Uses JavaFX WebView cache
- **Web:** Uses browser's iframe cache

### Best Practices

1. **Limit concurrent WebViews** - each instance consumes memory
2. **Dispose when not needed** - remove from composition to trigger cleanup
3. **Avoid large HTML content** - split into smaller chunks if possible
4. **Use appropriate loading indicators** - improve perceived performance
