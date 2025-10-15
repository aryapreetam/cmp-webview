package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javafx.application.Platform as JavaFXPlatform

private var javaFXInitialized = false

private fun initJavaFX() {
  if (!javaFXInitialized) {
    javaFXInitialized = true
    SwingUtilities.invokeLater { JFXPanel() }
  }
}

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  initJavaFX()

  Box(modifier = modifier) {
    SwingPanel(
      factory = {
        val panel = JPanel(BorderLayout())
        val jfxPanel = JFXPanel()

        panel.add(jfxPanel, BorderLayout.CENTER)

        JavaFXPlatform.runLater {
          val webView = WebView()
          val engine = webView.engine

          // Create Java bridge object
          val bridge = object {
            @Suppress("unused")
            fun postMessage(message: String) {
              JavaFXPlatform.runLater {
                callbacks.onScriptResult?.invoke(message)
              }
            }
          }

          // Worker state listener
          engine.loadWorker.stateProperty().addListener { _, _, newState ->
            when (newState) {
              Worker.State.RUNNING -> {
                JavaFXPlatform.runLater {
                  callbacks.onLoadStarted?.invoke()
                }
              }

              Worker.State.SUCCEEDED -> {
                JavaFXPlatform.runLater {
                  try {
                    val window = engine.executeScript("window") as JSObject
                    window.setMember("javaBridge", bridge)
                    engine.executeScript(BRIDGE_SCRIPT)
                  } catch (e: Exception) {
                    println("Desktop bridge injection error: ${e.message}")
                  }
                  callbacks.onLoadFinished?.invoke()
                }
              }

              Worker.State.FAILED -> {
                JavaFXPlatform.runLater {
                  callbacks.onLoadError?.invoke(
                    engine.loadWorker.exception?.message ?: "Load failed"
                  )
                }
              }

              else -> {}
            }
          }

          // Load content
          when (content) {
            is WebViewContent.Url -> engine.load(content.url)
            is WebViewContent.Html -> engine.loadContent(content.htmlContent)
          }

          jfxPanel.scene = Scene(webView)
        }

        panel
      },
      modifier = Modifier.fillMaxSize()
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      // Cleanup if needed
    }
  }
}