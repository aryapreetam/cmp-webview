package io.github.aryapreetam.cmpwebview

import androidx.compose.runtime.Composable

@Composable
fun WebView(url: String, onScriptResult: ((String) -> Unit)? = null) {
  return WebViewImpl(url, onScriptResult)
}

@Composable
internal expect fun WebViewImpl(url: String, onScriptResult: ((String) -> Unit)? = null)
