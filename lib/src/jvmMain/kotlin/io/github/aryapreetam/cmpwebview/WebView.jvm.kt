package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javafx.application.Platform as JavaFXPlatform

private var javaFXInitialized = false

private fun initJavaFX() {
  if (!javaFXInitialized) {
    javaFXInitialized = true
    // Initialize JavaFX toolkit
    SwingUtilities.invokeLater {
      JFXPanel() // This initializes JavaFX
    }
  }
}


@androidx.compose.runtime.Composable
actual fun WebViewImpl(url: String, onScriptResult: ((String) -> Unit)?) {
  initJavaFX()
  val jfxPanel = remember { JFXPanel() }

  Box(modifier = Modifier.fillMaxSize()) {
    SwingPanel(
      factory = {
        val panel = JPanel(BorderLayout()).apply {
          add(jfxPanel, BorderLayout.CENTER)
        }

        JavaFXPlatform.runLater {
          val webView = WebView()
          if (onScriptResult != null) {
            webView.engine.setOnAlert { event ->
              onScriptResult(event.data)
            }
          }
          webView.engine.loadContent(url, "text/html")
          jfxPanel.scene = Scene(webView)
        }

        panel
      },
      modifier = Modifier.fillMaxSize()
    )
  }
}