package io.github.aryapreetam.cmpwebview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopHtmlContentStoreTest {

  @Test
  fun urlForHtml_returnsFileUrl_andUpdatesWhenHtmlChanges() {
    val store = DesktopHtmlContentStore()

    val url1 = store.urlForHtml("""
      <!doctype html>
      <html><body><h1>Hello</h1></body></html>
    """.trimIndent())

    assertTrue(url1.startsWith("file:"), "Expected file URL, got: $url1")

    val url2 = store.urlForHtml("""
      <!doctype html>
      <html><body><h1>Hello again</h1></body></html>
    """.trimIndent())

    assertTrue(url2.startsWith("file:"), "Expected file URL, got: $url2")

    // Store should reuse the same temp file for a given instance, but change the URL (fragment)
    // so the WebView can detect content changes and reload.
    assertEquals(url1.substringBefore('#'), url2.substringBefore('#'))
    assertTrue(url1 != url2)
  }
}
