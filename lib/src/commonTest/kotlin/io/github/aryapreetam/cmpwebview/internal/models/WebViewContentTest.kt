package io.github.aryapreetam.cmpwebview.internal.models

import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Unit tests for WebViewContent sealed class validation.
 */
class WebViewContentTest {

  @Test
  fun `Url with valid HTTPS URL should create successfully`() {
    val content = WebViewContent.Url("https://example.com")
    assertNotNull(content)
    assertEquals("https://example.com", content.url)
  }

  @Test
  fun `Url with valid HTTP URL should create successfully`() {
    val content = WebViewContent.Url("http://example.com")
    assertNotNull(content)
    assertEquals("http://example.com", content.url)
  }

  @Test
  fun `Url with headers should store headers correctly`() {
    val headers = mapOf("Authorization" to "Bearer token123")
    val content = WebViewContent.Url("https://api.example.com", headers)
    assertEquals(headers, content.headers)
  }

  @Test
  fun `Url with blank URL should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("")
    }
  }

  @Test
  fun `Url with whitespace-only URL should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("   ")
    }
  }

  @Test
  fun `Url with javascript scheme should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("javascript:alert('XSS')")
    }
  }

  @Test
  fun `Url with JavaScript scheme uppercase should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("JavaScript:alert('XSS')")
    }
  }

  @Test
  fun `Url with vbscript scheme should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("vbscript:msgbox('XSS')")
    }
  }

  @Test
  fun `Url with file scheme should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("file:///etc/passwd")
    }
  }

  @Test
  fun `Url with data scheme should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Url("data:text/html,<script>alert('XSS')</script>")
    }
  }

  @Test
  fun `Html with valid HTML content should create successfully`() {
    val html = "<html><body>Hello World</body></html>"
    val content = WebViewContent.Html(html)
    assertNotNull(content)
    assertEquals(html, content.htmlContent)
  }

  @Test
  fun `Html with baseUrl should store baseUrl correctly`() {
    val html = "<html><body><a href='/page'>Link</a></body></html>"
    val baseUrl = "https://example.com"
    val content = WebViewContent.Html(html, baseUrl)
    assertEquals(baseUrl, content.baseUrl)
  }

  @Test
  fun `Html with blank content should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Html("")
    }
  }

  @Test
  fun `Html with whitespace-only content should throw IllegalArgumentException`() {
    assertFailsWith<IllegalArgumentException> {
      WebViewContent.Html("   ")
    }
  }
}
