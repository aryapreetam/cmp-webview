package io.github.aryapreetam.cmpwebview

import kotlin.test.Test
import kotlin.test.assertTrue
import java.io.File
import java.net.URI

class DesktopHtmlContentStoreTest {

  @Test
  fun urlForHtml_returnsFileUrl_andChangesWhenHtmlChanges() {
    val store = DesktopHtmlContentStore()

    val url1 = store.urlForHtml("""
      <!doctype html>
      <html><body><h1>Hello</h1></body></html>
    """.trimIndent(), baseUrl = null)

    assertTrue(url1.startsWith("file:"), "Expected file URL, got: $url1")

    val url2 = store.urlForHtml("""
      <!doctype html>
      <html><body><h1>Hello again</h1></body></html>
    """.trimIndent(), baseUrl = null)

    assertTrue(url2.startsWith("file:"), "Expected file URL, got: $url2")

    // Content changes should produce a different URL path to force a real reload.
    assertTrue(url1 != url2)
  }

  @Test
  fun urlForHtml_injectsBaseHrefWhenBaseUrlProvided() {
    val store = DesktopHtmlContentStore()

    val url = store.urlForHtml(
      html = """
        <!doctype html>
        <html>
          <head><title>BaseUrl</title></head>
          <body>
            <a href="page2.html">rel</a>
          </body>
        </html>
      """.trimIndent(),
      baseUrl = "https://example.com/"
    )

    assertTrue(url.startsWith("file:"), "Expected file URL, got: $url")

    val file = File(URI(url))
    assertTrue(file.exists(), "Expected temp html file to exist")
    val written = file.readText(Charsets.UTF_8)
    assertTrue(
      written.contains("<base href=\"https://example.com/\">"),
      "Expected injected <base> tag. File contents: $written"
    )
  }
}
