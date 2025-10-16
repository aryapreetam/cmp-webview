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
        // Create browser resources that cleanup properly when content changes
        val browserResources = remember(content) {
          BrowserResources()
        }

        DisposableEffect(content) {
          onDispose {
            browserResources.cleanup(client)
          }
        }

        // Key ensures SwingPanel is recreated when content changes
        key(content) {
          SwingPanel(
            factory = {
              val panel = JPanel(BorderLayout())
              val kcefClient = client!!

              // Create load handler FIRST, before browser creation
              val handler = object : CefLoadHandlerAdapter() {
                override fun onLoadStart(
                  browser: CefBrowser?,
                  frame: CefFrame?,
                  transitionType: CefRequest.TransitionType?
                ) {
                  val f = frame ?: return
                  if (!f.isMain) return
                  callbacks.onLoadStarted?.invoke()
                }

                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                  val f = frame ?: return
                  if (!f.isMain) return
                  // Inject bridge script (minimal, no logs)
                  val bridgeSetupScript = """
                    window.javaBridge = {
                      postMessage: function(message) {
                        if (typeof window.cefQuery === 'function') {
                          window.cefQuery({
                            request: 'bridge:' + message,
                            persistent: false
                          });
                        }
                      }
                    };
                    $BRIDGE_SCRIPT
                  """.trimIndent()

                  val scriptUrl = f.url ?: "about:blank"
                  f.executeJavaScript(bridgeSetupScript, scriptUrl, 0)
                  callbacks.onLoadFinished?.invoke()
                }

                override fun onLoadError(
                  browser: CefBrowser?,
                  frame: CefFrame?,
                  errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
                  errorText: String?,
                  failedUrl: String?
                ) {
                  val f = frame ?: return
                  if (!f.isMain) return
                  callbacks.onLoadError?.invoke("$errorCode: $errorText ($failedUrl)")
                }
              }

              // Add load handler BEFORE creating browser
              kcefClient.addLoadHandler(handler)
              browserResources.loadHandler = handler

              // Create and add message router BEFORE creating the browser
              val msgRouterConfig = CefMessageRouter.CefMessageRouterConfig(
                "cefQuery",
                "cefQueryCancel"
              )
              val msgRouter = CefMessageRouter.create(msgRouterConfig)
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
                    val message = request.substring(7)
                    callbacks.onScriptResult?.invoke(message)
                    callback?.success("")
                    return true
                  }
                  return false
                }
              }, true)
              kcefClient.addMessageRouter(msgRouter)
              browserResources.messageRouter = msgRouter

              val url = when (content) {
                is WebViewContent.Url -> content.url
                is WebViewContent.Html -> {
                  "data:text/html;base64,${Base64.getEncoder().encodeToString(content.htmlContent.toByteArray())}"
                }
              }

              val browser = kcefClient.createBrowser(url, CefRendering.DEFAULT, false)
              browserResources.browser = browser

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
}

// Helper class to manage browser resources that need cleanup
private class BrowserResources {
  var browser: CefBrowser? = null
  var messageRouter: CefMessageRouter? = null
  var loadHandler: CefLoadHandlerAdapter? = null

  fun cleanup(client: KCEFClient?) {
    // Remove and dispose message router
    messageRouter?.let { router ->
      try {
        client?.removeMessageRouter(router)
        router.dispose()
      } catch (_: Exception) {
        // ignore
      }
    }
    messageRouter = null

    // Note: KCEF doesn't support removing individual load handlers
    loadHandler = null

    // Close the browser instance
    browser?.let { b ->
      try {
        b.close(true)
      } catch (_: Exception) {
        // ignore
      }
    }
    browser = null
  }
}