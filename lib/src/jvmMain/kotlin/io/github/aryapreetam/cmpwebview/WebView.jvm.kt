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

  var globalHandlersInstalled: Boolean = false
  val callbacksByBrowser: MutableMap<CefBrowser, WebViewCallbacks> =
    Collections.synchronizedMap(WeakHashMap())
  var messageRouter: CefMessageRouter? = null
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

    client?.let { installGlobalHandlersIfNeeded(it) }
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
        val kcefClient = client!!
        val browserResources = remember { BrowserResources() }

        // Keep the global browser→callbacks routing up to date.
        SideEffect {
          browserResources.browser?.let { b ->
            KCEFState.callbacksByBrowser[b] = callbacks
          }
        }

        // Navigate when content changes (avoid recreating the browser).
        LaunchedEffect(content, browserResources.browser) {
          val browser = browserResources.browser ?: return@LaunchedEffect
          val targetUrl = contentToDesktopUrl(content)
          if (browserResources.lastLoadedUrl != targetUrl) {
            browserResources.lastLoadedUrl = targetUrl
            browser.loadURL(targetUrl)
          }
        }

        // Cleanup only when leaving composition.
        DisposableEffect(Unit) {
          onDispose {
            browserResources.cleanup(kcefClient)
          }
        }

        SwingPanel(
          factory = {
            val panel = JPanel(BorderLayout())

            val initialUrl = contentToDesktopUrl(content)
            val browser = kcefClient.createBrowser(initialUrl, CefRendering.DEFAULT, false)
            browserResources.browser = browser
            browserResources.lastLoadedUrl = initialUrl

            // Register callbacks for this browser instance.
            KCEFState.callbacksByBrowser[browser] = callbacks

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

private class BrowserResources {
  var browser: CefBrowser? = null
  var lastLoadedUrl: String? = null

  fun cleanup(client: KCEFClient?) {
    browser?.let { b ->
      KCEFState.callbacksByBrowser.remove(b)
    }

    // Close the browser instance
    browser?.let { b ->
      try {
        b.close(true)
      } catch (_: Exception) {
        // ignore
      }
    }
    browser = null
    lastLoadedUrl = null
  }
}

private fun contentToDesktopUrl(content: WebViewContent): String {
  return when (content) {
    is WebViewContent.Url -> content.url
    is WebViewContent.Html -> {
      "data:text/html;base64,${Base64.getEncoder().encodeToString(content.htmlContent.toByteArray())}"
    }
  }
}

private fun installGlobalHandlersIfNeeded(kcefClient: KCEFClient) {
  if (KCEFState.globalHandlersInstalled) return
  KCEFState.globalHandlersInstalled = true

  // Global load handler: routes lifecycle events to the correct composable via browser mapping
  val loadHandler = object : CefLoadHandlerAdapter() {
    override fun onLoadStart(
      browser: CefBrowser?,
      frame: CefFrame?,
      transitionType: CefRequest.TransitionType?
    ) {
      val b = browser ?: return
      val f = frame ?: return
      if (!f.isMain) return
      KCEFState.callbacksByBrowser[b]?.onLoadStarted?.invoke()
    }

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      val b = browser ?: return
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

      KCEFState.callbacksByBrowser[b]?.onLoadFinished?.invoke()
    }

    override fun onLoadError(
      browser: CefBrowser?,
      frame: CefFrame?,
      errorCode: org.cef.handler.CefLoadHandler.ErrorCode?,
      errorText: String?,
      failedUrl: String?
    ) {
      val b = browser ?: return
      val f = frame ?: return
      if (!f.isMain) return
      KCEFState.callbacksByBrowser[b]?.onLoadError?.invoke("$errorCode: $errorText ($failedUrl)")
    }
  }
  kcefClient.addLoadHandler(loadHandler)

  // Global message router: routes JS→Compose messages using the same browser mapping
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
      val b = browser ?: return false
      if (request?.startsWith("bridge:") == true) {
        val message = request.substring(7)
        KCEFState.callbacksByBrowser[b]?.onScriptResult?.invoke(message)
        callback?.success("")
        return true
      }
      return false
    }
  }, true)
  kcefClient.addMessageRouter(msgRouter)
  KCEFState.messageRouter = msgRouter
}