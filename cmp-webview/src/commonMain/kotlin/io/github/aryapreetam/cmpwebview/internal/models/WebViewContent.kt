package io.github.aryapreetam.cmpwebview.internal.models

/**
 * Sealed class representing the content to load in the webview.
 * Internal use only - not exposed to library users.
 */
internal sealed class WebViewContent {
  /**
   * URL content with optional HTTP headers.
   *
   * @property url The URL to load (validated before use)
   * @property headers Optional HTTP headers for the request
   */
  data class Url(
    val url: String,
    val headers: Map<String, String>? = null
  ) : WebViewContent() {
    init {
      require(url.isNotBlank()) { "URL cannot be blank" }
      // Validate URL scheme - reject dangerous schemes
      val lowerUrl = url.trim().lowercase()
      require(!lowerUrl.startsWith("javascript:")) { "javascript: scheme is not allowed" }
      require(!lowerUrl.startsWith("vbscript:")) { "vbscript: scheme is not allowed" }
      require(!lowerUrl.startsWith("file:")) { "file: scheme is not allowed for security reasons" }
      require(!lowerUrl.startsWith("data:")) { "data: scheme is not allowed" }
    }
  }

  /**
   * HTML content with optional base URL for relative links.
   *
   * @property htmlContent The HTML string to display
   * @property baseUrl Optional base URL for resolving relative links
   */
  data class Html(
    val htmlContent: String,
    val baseUrl: String? = null
  ) : WebViewContent() {
    init {
      require(htmlContent.isNotBlank()) { "HTML content cannot be blank" }
    }
  }
}

