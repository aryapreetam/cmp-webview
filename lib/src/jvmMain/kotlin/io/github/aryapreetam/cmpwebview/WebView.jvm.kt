package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFClient
import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.models.WebViewCallbacks
import io.github.aryapreetam.cmpwebview.internal.models.WebViewContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefRendering
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.network.CefRequest
import java.awt.BorderLayout
import java.io.File
import java.util.*
import javax.swing.JPanel

// Global KCEF state - initialized once per application
private object KCEFState {
  var initialized = false
  var client: KCEFClient? = null
  var initError: String? = null
  var downloadProgress: Float = -1f
}

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier
) {
  var kcefInitialized by remember { mutableStateOf(KCEFState.initialized) }
  var downloadProgress by remember { mutableStateOf(KCEFState.downloadProgress) }
  var initError by remember { mutableStateOf(KCEFState.initError) }
  var client by remember { mutableStateOf(KCEFState.client) }

  // Initialize KCEF once globally
  LaunchedEffect(Unit) {
    if (!KCEFState.initialized && KCEFState.initError == null) {
      withContext(Dispatchers.IO) {
        try {
          val bundleLocation = System.getProperty("compose.application.resources.dir")?.let { File(it) }
            ?: File(System.getProperty("user.home"))

          KCEF.init(
            builder = {
              installDir(File(bundleLocation, "kcef-bundle"))
              progress {
                onDownloading {
                  KCEFState.downloadProgress = it
                  downloadProgress = it
                }
                onInitialized {
                  KCEFState.initialized = true
                  kcefInitialized = true
                }
              }
            },
            onError = { error ->
              val errorMsg = error?.message ?: "KCEF initialization failed"
              KCEFState.initError = errorMsg
              initError = errorMsg
              callbacks.onLoadError?.invoke(errorMsg)
            },
            onRestartRequired = {
              val errorMsg = "Application restart required to load KCEF. Please restart the application."
              KCEFState.initError = errorMsg
              initError = errorMsg
            }
          )
        } catch (e: Exception) {
          val errorMsg = "KCEF initialization error: ${e.message}"
          KCEFState.initError = errorMsg
          initError = errorMsg
          callbacks.onLoadError?.invoke(errorMsg)
        }
      }
    } else if (KCEFState.initialized) {
      // Already initialized, update local state
      kcefInitialized = true
      client = KCEFState.client
    } else if (KCEFState.initError != null) {
      // Previous initialization failed, update local state
      initError = KCEFState.initError
    }
  }

  // Create client after initialization (only once globally)
  LaunchedEffect(kcefInitialized) {
    if (kcefInitialized && KCEFState.client == null) {
      withContext(Dispatchers.IO) {
        KCEFState.client = KCEF.newClientOrNull { error ->
          val errorMsg = error?.message ?: "Failed to create KCEF client"
          KCEFState.initError = errorMsg
          initError = errorMsg
          callbacks.onLoadError?.invoke(errorMsg)
        }
        client = KCEFState.client
      }
    } else if (kcefInitialized) {
      // Client already exists, use it
      client = KCEFState.client
    }
  }

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    when {
      initError != null -> {
        BasicText("WebView Error: $initError")
      }
      !kcefInitialized -> {
        if (downloadProgress >= 0) {
          BasicText("Downloading Chromium: ${downloadProgress.toInt()}%")
        } else {
          BasicText("Initializing...")
        }
      }
      client == null -> {
        BasicText("Creating browser...")
      }
      else -> {
        // Key ensures SwingPanel is recreated when content changes
        key(content) {
          SwingPanel(
            factory = {
              val panel = JPanel(BorderLayout())
              val kcefClient = client!!
              val browser = kcefClient.createBrowser(
                when (content) {
                  is WebViewContent.Url -> content.url
                  is WebViewContent.Html -> {
                    "data:text/html;base64,${Base64.getEncoder().encodeToString(content.htmlContent.toByteArray())}"
                  }
                },
                CefRendering.DEFAULT,
                false
              )

              // Add load handler
              kcefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                  browser: CefBrowser?,
                  frame: CefFrame?,
                  transitionType: CefRequest.TransitionType?
                ) {
                  if (frame?.isMain == true) {
                    callbacks.onLoadStarted?.invoke()
                  }
                }

                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                  if (frame?.isMain == true) {
                    // Inject bridge script
                    frame.executeJavaScript(
                      """
                      window.javaBridge = {
                        postMessage: function(message) {
                          window.cefQuery({
                            request: 'bridge:' + message,
                            persistent: false,
                            onSuccess: function(response) {},
                            onFailure: function(error_code, error_message) {}
                          });
                        }
                      };
                      $BRIDGE_SCRIPT
                      """.trimIndent(),
                      frame.url,
                      0
                    )
                    callbacks.onLoadFinished?.invoke()
                  }
                }

                override fun onLoadError(
                  browser: CefBrowser?,
                  frame: CefFrame?,
                  errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
                  errorText: String?,
                  failedUrl: String?
                ) {
                  if (frame?.isMain == true) {
                    callbacks.onLoadError?.invoke("$errorCode: $errorText ($failedUrl)")
                  }
                }
              })

              // Add message router for bridge communication
              val msgRouter = CefMessageRouter.create()
              msgRouter.addHandler(object : CefMessageRouterHandlerAdapter() {
                override fun onQuery(
                  browser: CefBrowser?,
                  frame: CefFrame?,
                  queryId: Long,
                  request: String?,
                  persistent: Boolean,
                  callback: CefQueryCallback?
                ): Boolean {
                  if (request?.startsWith("bridge:") == true) {
                    val message = request.substring(7) // Remove "bridge:" prefix
                    callbacks.onScriptResult?.invoke(message)
                    callback?.success("")
                    return true
                  }
                  return false
                }
              }, true)
              kcefClient.addMessageRouter(msgRouter)

              panel.add(browser.uiComponent, BorderLayout.CENTER)
              panel
            },
            update = {},
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      // Don't dispose the global client
    }
  }
}