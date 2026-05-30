package io.github.aryapreetam.cmpwebview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import io.github.aryapreetam.cmpwebview.internal.bridge.unwrapBridgeMessage
import io.github.aryapreetam.cmpwebview.internal.bridge.resolveBridgeEnablement
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.AWTEvent
import java.awt.BorderLayout
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.WindowEvent
import java.io.File
import java.util.*
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer

private suspend fun <T> runOnSwingEdt(block: () -> T): T {
  if (SwingUtilities.isEventDispatchThread()) return block()

  return suspendCancellableCoroutine { continuation ->
    SwingUtilities.invokeLater {
      if (!continuation.isActive) return@invokeLater
      try {
        continuation.resume(block())
      } catch (t: Throwable) {
        continuation.resumeWithException(t)
      }
    }
  }
}

// Global KCEF state - initialized once per application
private object KCEFState {
  var initialized = false
  var client: KCEFClient? = null
  var initError: String? = null
  var downloadProgress: Float = -1f

  var globalHandlersInstalled: Boolean = false
  // NOTE: JCEF may pass different Java wrapper objects for the same underlying browser in
  // callbacks, so we must key by a stable identifier rather than `CefBrowser` object identity.
  val callbacksByBrowserId: MutableMap<Int, WebViewCallbacks> =
    Collections.synchronizedMap(mutableMapOf())
  val bridgeEnabledByBrowserId: MutableMap<Int, Boolean> =
    Collections.synchronizedMap(mutableMapOf())

  // When a browser is created at `about:blank`, we sometimes need to defer the *first* real
  // navigation until after the internal initial `about:blank` load completes.
  val pendingFirstNavigationByBrowserId: MutableMap<Int, String> =
    Collections.synchronizedMap(mutableMapOf())
  var messageRouter: CefMessageRouter? = null
}

private fun desktopDebugEnabled(): Boolean {
  return System.getProperty("cmpwebview.desktop.debug") == "true" ||
    System.getenv("CMPWEBVIEW_DESKTOP_DEBUG") == "true"
}

private fun desktopLog(message: String) {
  if (!desktopDebugEnabled()) return
  println("[cmp-webview][desktop] $message")
}

@Composable
internal actual fun WebViewImpl(
  content: WebViewContent,
  callbacks: WebViewCallbacks,
  modifier: Modifier,
  options: WebViewOptions,
  controller: WebViewController?
) {
  var kcefInitialized by remember { mutableStateOf(KCEFState.initialized) }
  var downloadProgress by remember { mutableStateOf(KCEFState.downloadProgress) }
  var initError by remember { mutableStateOf(KCEFState.initError) }
  var client by remember { mutableStateOf(KCEFState.client) }

  // Initialize KCEF once globally
  LaunchedEffect(Unit) {
    if (!KCEFState.initialized && KCEFState.initError == null) {
      if (System.getProperty("cmpwebview.testmode") == "true") {
        org.cef.CefApp.addAppHandler(object : org.cef.handler.CefAppHandlerAdapter(null) {
          override fun onBeforeCommandLineProcessing(process_type: String?, command_line: org.cef.callback.CefCommandLine?) {
            command_line?.appendArgument("--no-sandbox")
            command_line?.appendArgument("--disable-gpu")
          }
        })
      }
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

        val bridgeEnablement = remember(options, callbacks.onScriptResult, controller) {
          resolveBridgeEnablement(options, callbacks, controller)
        }

        DisposableEffect(controller, browserResources.browser) {
          val impl = controller as? WebViewControllerImpl
          val browser = browserResources.browser

          if (impl != null && browser != null) {
            impl.attach(
              WebViewControllerImpl.Bindings(
                evaluateJavaScript = { script ->
                  try {
                    // Best-effort: CEF doesn't expose a simple return-value callback for JS evaluation.
                    runOnSwingEdt {
                      val scriptUrl = browser.url ?: "about:blank"
                      browser.executeJavaScript(script, scriptUrl, 0)
                    }
                    WebViewJsResult.Unsupported(
                      "Desktop evaluateJavaScript return values are not supported yet (no callback channel wired). Script was executed best-effort."
                    )
                  } catch (t: Throwable) {
                    WebViewJsResult.Error("evaluateJavaScript failed", t)
                  }
                },
                reload = { SwingUtilities.invokeLater { browser.reload() } },
                goBack = {
                  if (!browser.canGoBack()) return@Bindings false
                  SwingUtilities.invokeLater { browser.goBack() }
                  true
                },
                goForward = {
                  if (!browser.canGoForward()) return@Bindings false
                  SwingUtilities.invokeLater { browser.goForward() }
                  true
                }
              )
            )
          }

          onDispose {
            impl?.detach()
          }
        }

        // Keep the global browser→callbacks routing up to date.
        SideEffect {
          browserResources.browser?.let { b ->
            val id = b.identifier
            KCEFState.callbacksByBrowserId[id] = callbacks
            KCEFState.bridgeEnabledByBrowserId[id] = bridgeEnablement.jsToCompose
          }
        }

        // Navigate when content changes (avoid recreating the browser).
        LaunchedEffect(content, browserResources.browser) {
          val targetUrl = contentToDesktopUrl(content, browserResources)
          if (browserResources.lastLoadedUrl != targetUrl) {
            browserResources.lastLoadedUrl = targetUrl
            browserResources.requestLoad(targetUrl)
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

            // Ensure load/message handlers are installed before the first browser instance is created.
            // Otherwise the initial load can miss onLoadEnd, which is where we inject the bridge and
            // propagate onLoadFinished.
            installGlobalHandlersIfNeeded(kcefClient)

            val initialUrl = contentToDesktopUrl(content, browserResources)

            browserResources.container = panel
            browserResources.client = kcefClient
            browserResources.initialCallbacks = callbacks
            browserResources.initialBridgeEnabled = bridgeEnablement.jsToCompose
            browserResources.lastLoadedUrl = initialUrl

            // Start initial navigation after wiring is in place.
            browserResources.requestLoad(initialUrl)
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
  var browser: CefBrowser? by mutableStateOf(null)
  var lastLoadedUrl: String? = null

  var container: JPanel? = null
  var client: KCEFClient? = null

  // Used only for initial browser creation wiring; normal updates go through `SideEffect`.
  var initialCallbacks: WebViewCallbacks? = null
  var initialBridgeEnabled: Boolean = false

  private var hierarchyListener: HierarchyListener? = null
  private var pendingUrl: String? = null
  private var showPollTimer: Timer? = null

  private var browserIdReadyTimer: Timer? = null
  private var stableBrowserId: Int? = null
  private var pendingFirstNavigationAwaitingId: String? = null

  val htmlContentStore: DesktopHtmlContentStore = DesktopHtmlContentStore()

  fun requestLoad(url: String) {
    val panel = container
    if (panel == null) {
      val b = browser
      if (b != null) {
        desktopLog("requestLoad immediate (panel=null): $url")
        b.loadURL(url)
      } else {
        // No container and no browser yet. Queue and wait for composition to provide a container.
        pendingUrl = url
        desktopLog("requestLoad queued (panel=null, browser=null): $url")
      }
      return
    }

    // Always keep the latest requested URL.
    pendingUrl = url

    fun panelReady(): Boolean {
      // `isShowing` alone is not always sufficient on Compose Desktop / SwingPanel.
      // We've seen cases where navigation happens while the component is still effectively
      // unrealized (e.g., size is still 0), leading to a blank first render.
      return panel.isShowing && panel.width > 0 && panel.height > 0
    }

    fun ensureBrowserCreated(): CefBrowser? {
      val existing = browser
      if (existing != null) return existing

      val kcefClient = client
      if (kcefClient == null) {
        desktopLog("ensureBrowserCreated skipped (client=null)")
        return null
      }

      desktopLog("ensureBrowserCreated creating browser (panel ready; size=${panel.width}x${panel.height})")
      val created = kcefClient.createBrowser("about:blank", CefRendering.DEFAULT, false)
      browser = created
      desktopLog("ensureBrowserCreated created browser id=${created.identifier}")

      // `created.identifier` may initially be `-1` and become valid shortly after.
      // Defer all identifier-keyed registrations until we observe a valid id.
      startBrowserIdReadyWatcher(created)

      panel.add(created.uiComponent, BorderLayout.CENTER)
      panel.revalidate()
      panel.repaint()

      return created
    }

    fun tryExecuteDeferredLoad() {
      if (!panelReady()) return

      val browserBefore = browser
      val b = ensureBrowserCreated() ?: return
      val createdNow = browserBefore == null

      hierarchyListener?.let { listener ->
        try {
          panel.removeHierarchyListener(listener)
        } catch (_: Exception) {
          // ignore
        }
        hierarchyListener = null
      }

      showPollTimer?.let { t ->
        try {
          t.stop()
        } catch (_: Exception) {
          // ignore
        }
        showPollTimer = null
      }

      val toLoad = pendingUrl
      pendingUrl = null
      if (toLoad != null) {
        if (createdNow) {
          // On some setups, a just-created browser will perform its internal initial `about:blank`
          // navigation *after* we request the real URL, effectively overriding it.
          // Queue the first real navigation to be triggered from `onLoadEnd(about:blank)`.
          desktopLog("requestLoad queued first navigation after about:blank: $toLoad")
          desktopLog("requestLoad pendingFirstNavigation id=${b.identifier}")
          pendingFirstNavigationAwaitingId = toLoad
          startBrowserIdReadyWatcher(b)
        } else {
          desktopLog("requestLoad executing load: $toLoad")
          SwingUtilities.invokeLater {
            try {
              b.loadURL(toLoad)
            } catch (t: Throwable) {
              desktopLog("requestLoad deferred load failed: ${t.message}")
            }
          }
        }
      }
    }

    if (panelReady() && browser != null) {
      desktopLog(
        "requestLoad immediate (panel ready; size=${panel.width}x${panel.height}): $url"
      )
      val b = browser
      pendingUrl = null
      if (b != null) {
        b.loadURL(url)
      }
      return
    }

    // Defer navigation (and possibly browser creation) until the component is ready.
    desktopLog(
      "requestLoad deferred (panel not ready; showing=${panel.isShowing} size=${panel.width}x${panel.height}): $url"
    )

    if (hierarchyListener == null) {
      val listener = object : HierarchyListener {
        override fun hierarchyChanged(e: HierarchyEvent?) {
          tryExecuteDeferredLoad()
        }
      }

      hierarchyListener = listener
      panel.addHierarchyListener(listener)

      // Handle the case where we attach the listener after the panel already became visible.
      SwingUtilities.invokeLater {
        tryExecuteDeferredLoad()
      }
    }

    // Fallback: some environments don't reliably deliver a hierarchy event at the exact moment
    // `isShowing` flips to true for the SwingPanel content. Polling is cheap and makes the
    // first navigation reliable.
    if (showPollTimer == null) {
      SwingUtilities.invokeLater {
        if (showPollTimer == null) {
          showPollTimer = Timer(50) {
            tryExecuteDeferredLoad()
          }.also {
            it.isRepeats = true
            it.start()
          }
        }
      }
    }

    // Try immediately in case we're already ready and just missed the right event.
    SwingUtilities.invokeLater {
      tryExecuteDeferredLoad()
    }
  }

  fun cleanup(client: KCEFClient?) {
    browser?.let { b ->
      val id = stableBrowserId ?: b.identifier
      KCEFState.callbacksByBrowserId.remove(id)
      KCEFState.bridgeEnabledByBrowserId.remove(id)
      KCEFState.pendingFirstNavigationByBrowserId.remove(id)
    }

    // Close the browser instance
    browser?.let { b ->
      try {
        b.close(true)
      } catch (_: Exception) {
        // ignore
      }
    }

    htmlContentStore.cleanup()

    showPollTimer?.let {
      try {
        it.stop()
      } catch (_: Exception) {
        // ignore
      }
    }
    showPollTimer = null

    browserIdReadyTimer?.let {
      try {
        it.stop()
      } catch (_: Exception) {
        // ignore
      }
    }
    browserIdReadyTimer = null

    container?.let { panel ->
      hierarchyListener?.let { listener ->
        try {
          panel.removeHierarchyListener(listener)
        } catch (_: Exception) {
          // ignore
        }
      }
    }

    browser = null
    lastLoadedUrl = null
    pendingUrl = null
    hierarchyListener = null
    container = null
    this.client = null
    initialCallbacks = null
    initialBridgeEnabled = false
    pendingFirstNavigationAwaitingId = null
    stableBrowserId = null
  }

  private fun startBrowserIdReadyWatcher(browser: CefBrowser) {
    if (browserIdReadyTimer != null) return

    browserIdReadyTimer = Timer(10) {
      val id = browser.identifier
      if (id == -1) return@Timer

      stableBrowserId = id
      desktopLog("browser identifier ready: $id")

      // Initial registration. (SideEffect will keep it up-to-date.)
      initialCallbacks?.let { cb ->
        KCEFState.callbacksByBrowserId[id] = cb
      }
      KCEFState.bridgeEnabledByBrowserId[id] = initialBridgeEnabled

      pendingFirstNavigationAwaitingId?.let { url ->
        desktopLog("transferring pending first navigation to id=$id: $url")
        KCEFState.pendingFirstNavigationByBrowserId[id] = url
        pendingFirstNavigationAwaitingId = null
      }

      browserIdReadyTimer?.stop()
      browserIdReadyTimer = null
    }.also {
      it.isRepeats = true
      it.start()
    }
  }
}

/**
 * Stores inline HTML in a temp file and returns a `file://` URL.
 *
 * Rationale: `data:` URLs are not reliably supported across all KCEF/JCEF setups.
 */
internal class DesktopHtmlContentStore {
  private var tempHtmlFile: File? = null
  private data class Signature(
    val hash: Int,
    val length: Int,
    val baseUrl: String?,
  )

  private var lastSignature: Signature? = null

  fun urlForHtml(html: String, baseUrl: String?): String {
    val processedHtml = if (baseUrl != null) {
      injectBaseHrefIfMissing(html, baseUrl)
    } else {
      html
    }

    val signature = Signature(
      hash = processedHtml.hashCode(),
      length = processedHtml.length,
      baseUrl = baseUrl,
    )

    val existing = tempHtmlFile
    val file = if (existing == null || lastSignature != signature) {
      // IMPORTANT: do not rely on URL fragments to force reload.
      // Navigations that only change `#fragment` often don't reload the document.
      // Instead, create a new temp file when content changes so the URL path changes.
      existing?.let {
        try {
          it.delete()
        } catch (_: Exception) {
          // ignore
        }
      }

      File.createTempFile("cmpwebview-", ".html").also {
        it.deleteOnExit()
        it.writeText(processedHtml, Charsets.UTF_8)
        tempHtmlFile = it
        lastSignature = signature
      }
    } else {
      existing
    }

    return file.toURI().toString()
  }

  fun cleanup() {
    tempHtmlFile?.let {
      try {
        it.delete()
      } catch (_: Exception) {
        // ignore
      }
    }
    tempHtmlFile = null
    lastSignature = null
  }
}

private fun injectBaseHrefIfMissing(html: String, baseUrl: String): String {
  // If caller already provided a base tag, don't add another.
  if (Regex("<\\s*base\\b", RegexOption.IGNORE_CASE).containsMatchIn(html)) return html

  // Basic attribute escaping.
  val safeBaseUrl = baseUrl.replace("\"", "&quot;")
  val baseTag = "<base href=\"$safeBaseUrl\">"

  // Prefer inserting inside <head>.
  val headOpen = Regex("<\\s*head\\b[^>]*>", RegexOption.IGNORE_CASE)
  val match = headOpen.find(html)
  if (match != null) {
    val insertAt = match.range.last + 1
    return buildString(html.length + baseTag.length + 1) {
      append(html, 0, insertAt)
      append('\n')
      append(baseTag)
      append(html, insertAt, html.length)
    }
  }

  // No <head> tag found; prepend a minimal head.
  return "<head>\n$baseTag\n</head>\n$html"
}

private fun contentToDesktopUrl(content: WebViewContent, resources: BrowserResources): String {
  return when (content) {
    is WebViewContent.Url -> {
      // Ensure we don't keep stale temp files around when switching away from Html content.
      resources.htmlContentStore.cleanup()
      content.url
    }
    is WebViewContent.Html -> resources.htmlContentStore.urlForHtml(content.htmlContent, content.baseUrl)
  }
}

private fun installGlobalHandlersIfNeeded(kcefClient: KCEFClient) {
  if (KCEFState.globalHandlersInstalled) return
  KCEFState.globalHandlersInstalled = true
  desktopLog("installGlobalHandlersIfNeeded: installing global handlers")

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

      val id = b.identifier

      desktopLog("onLoadStart id=$id url=${f.url ?: b.url}")
      KCEFState.callbacksByBrowserId[id]?.onLoadStarted?.invoke()
    }

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      val b = browser ?: return
      val f = frame ?: return
      if (!f.isMain) return

      val id = b.identifier

      desktopLog("onLoadEnd id=$id status=$httpStatusCode url=${f.url ?: b.url}")

      // If we created this browser at `about:blank` and queued the first real navigation,
      // trigger it only after the internal `about:blank` load completes.
      val pendingFirstNav = KCEFState.pendingFirstNavigationByBrowserId.remove(id)
      val currentUrl = f.url ?: b.url
      desktopLog("onLoadEnd pendingFirstNav=${pendingFirstNav != null} currentUrl=$currentUrl")
      if (pendingFirstNav != null && currentUrl?.startsWith("about:blank") == true) {
        desktopLog("onLoadEnd about:blank -> triggering pending navigation: $pendingFirstNav")
        SwingUtilities.invokeLater {
          try {
            b.loadURL(pendingFirstNav)
          } catch (t: Throwable) {
            desktopLog("pending first navigation failed: ${t.message}")
          }
        }
        // Don't report load-finished for the internal blank page.
        return
      }

      val bridgeEnabled = KCEFState.bridgeEnabledByBrowserId[id] == true

      if (bridgeEnabled) {
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
      }
      KCEFState.callbacksByBrowserId[id]?.onLoadFinished?.invoke()
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

      val id = b.identifier

      desktopLog("onLoadError code=$errorCode text=$errorText url=$failedUrl")
      KCEFState.callbacksByBrowserId[id]?.onLoadError?.invoke("$errorCode: $errorText ($failedUrl)")
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
      val id = b.identifier
      if (request?.startsWith("bridge:") == true && KCEFState.bridgeEnabledByBrowserId[id] == true) {
        val message = request.substring(7)
        val payload = unwrapBridgeMessage(message)
        KCEFState.callbacksByBrowserId[id]?.onScriptResult?.invoke(payload)
        callback?.success("")
        return true
      }
      return false
    }
  }, true)
  kcefClient.addMessageRouter(msgRouter)
  KCEFState.messageRouter = msgRouter
}
