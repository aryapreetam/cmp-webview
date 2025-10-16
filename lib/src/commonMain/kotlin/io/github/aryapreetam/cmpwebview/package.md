# Package io.github.aryapreetam.cmpwebview

A comprehensive WebView library for Compose Multiplatform that enables displaying web content across all supported platforms.

## Overview

This package provides a unified WebView component that works consistently across Android, iOS, Desktop (JVM), and Web (WASM) platforms. The main entry point is the [WebView] composable function.

## Main Components

### WebView Composable

The primary component for displaying web content. It supports:
- Loading remote URLs
- Displaying HTML content directly
- Bidirectional JavaScript bridge communication
- Loading state callbacks
- Error handling
- Custom headers for HTTP requests

## Usage Examples

### Basic URL Loading

```kotlin
WebView(
    url = "https://example.com",
    modifier = Modifier.fillMaxSize()
)
```

### HTML Content

```kotlin
WebView(
    htmlContent = "<html><body><h1>Hello World</h1></body></html>",
    modifier = Modifier.fillMaxSize()
)
```

### With Callbacks

```kotlin
WebView(
    url = "https://example.com",
    onLoadStarted = { println("Loading started") },
    onLoadFinished = { println("Loading finished") },
    onLoadError = { error -> println("Error: $error") },
    modifier = Modifier.fillMaxSize()
)
```

### JavaScript Bridge

```kotlin
WebView(
    url = "https://example.com",
    onScriptResult = { message ->
        println("Message from JavaScript: $message")
    },
    modifier = Modifier.fillMaxSize()
)
```

## Platform Support

All platforms are fully supported with native implementations:
- **Android**: Uses native Android WebView
- **iOS**: Uses WKWebView
- **Desktop (JVM)**: Uses JavaFX WebView
- **Web (WASM)**: Uses HTML iframe

## Security Considerations

The library includes built-in security protections:
- Automatically rejects dangerous URL schemes (javascript:, vbscript:, file:)
- Always validate and sanitize URLs from user input
- Use HTTPS URLs in production
- Validate messages received from JavaScript bridge

## Accessibility

The WebView component provides full accessibility support including:
- Screen reader compatibility
- Semantic descriptions of loading states
- Proper content descriptions

## Testing

Use standard Compose testing with test tags:

```kotlin
WebView(
    url = "https://example.com",
    modifier = Modifier.testTag("my-webview")
)
```

## See Also

- [GitHub Repository](https://github.com/aryapreetam/cmp-webview) for source code and examples
