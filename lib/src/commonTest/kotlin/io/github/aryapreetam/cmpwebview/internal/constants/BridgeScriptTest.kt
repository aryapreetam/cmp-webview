package io.github.aryapreetam.cmpwebview.internal.constants

import io.github.aryapreetam.cmpwebview.internal.constants.BRIDGE_SCRIPT
import io.github.aryapreetam.cmpwebview.internal.bridge.BRIDGE_ENVELOPE_PREFIX
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for BRIDGE_SCRIPT constant.
 * Validates script structure and key components.
 */
class BridgeScriptTest {

  @Test
  fun `Bridge script should not be empty`() {
    assertTrue(BRIDGE_SCRIPT.isNotEmpty(), "Bridge script should contain code")
  }

  @Test
  fun `Bridge script should define ComposeWebViewBridge`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("window.ComposeWebViewBridge"),
      "Script should define window.ComposeWebViewBridge"
    )
  }

  @Test
  fun `Bridge script should define postMessage function`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("postMessage:"),
      "Script should define postMessage function"
    )
  }

  @Test
  fun `Bridge script should have double initialization check`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("if (window.ComposeWebViewBridge)"),
      "Script should check if already initialized"
    )
  }

  @Test
  fun `Bridge script should define MAX_MESSAGE_SIZE constant`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("MAX_MESSAGE_SIZE"),
      "Script should define message size limit"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("10 * 1024 * 1024"),
      "Message size limit should be 10MB"
    )
  }

  @Test
  fun `Bridge script should embed envelope prefix`() {
    assertTrue(
      BRIDGE_SCRIPT.contains(BRIDGE_ENVELOPE_PREFIX),
      "Script should include the Kotlin envelope prefix to keep receivers in sync"
    )
  }

  @Test
  fun `Bridge script should have sanitizeMessage function`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("function sanitizeMessage"),
      "Script should define sanitizeMessage function"
    )
  }

  @Test
  fun `Bridge script should check message size in sanitization`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("if (messageStr.length > MAX_MESSAGE_SIZE)"),
      "Script should validate message size"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("truncating"),
      "Script should truncate oversized messages"
    )
  }

  @Test
  fun `Bridge script should sanitize control characters`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("replace(/[\\x00-\\x1F\\x7F]/g, '')"),
      "Script should remove control characters"
    )
  }

  @Test
  fun `Bridge script should detect Android platform`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("window.AndroidBridge"),
      "Script should check for Android bridge"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("window.AndroidBridge.postMessage"),
      "Script should call Android bridge postMessage"
    )
  }

  @Test
  fun `Bridge script should detect iOS platform`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("window.webkit"),
      "Script should check for iOS webkit"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("window.webkit.messageHandlers.iosBridge"),
      "Script should access iOS message handler"
    )
  }

  @Test
  fun `Bridge script should detect Desktop platform`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("window.javaBridge"),
      "Script should check for JavaFX bridge"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("window.javaBridge.postMessage"),
      "Script should call Desktop bridge postMessage"
    )
  }

  @Test
  fun `Bridge script should detect Web platform`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("window.parent"),
      "Script should check for parent window"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("window.parent.postMessage"),
      "Script should use postMessage for Web"
    )
  }

  @Test
  fun `Bridge script should dispatch ComposeWebViewBridgeReady event`() {
    assertTrue(
      BRIDGE_SCRIPT.contains("ComposeWebViewBridgeReady"),
      "Script should dispatch ready event"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("new Event('ComposeWebViewBridgeReady')"),
      "Script should create Event object"
    )
    assertTrue(
      BRIDGE_SCRIPT.contains("window.dispatchEvent"),
      "Script should dispatch event"
    )
  }

  @Test
  fun `Bridge script should have error handling for each platform`() {
    // Should have try-catch blocks
    val tryCatchCount = BRIDGE_SCRIPT.split("try {").size - 1
    assertTrue(
      tryCatchCount >= 4,
      "Script should have try-catch for all 4 platforms (found $tryCatchCount)"
    )
  }

  @Test
  fun `Bridge script should wrap in IIFE to avoid global pollution`() {
    assertTrue(
      BRIDGE_SCRIPT.trim().startsWith("(function() {"),
      "Script should start with IIFE"
    )
    assertTrue(
      BRIDGE_SCRIPT.trim().endsWith("})();"),
      "Script should end with IIFE invocation"
    )
  }
}
