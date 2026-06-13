package io.github.aryapreetam.cmpwebview.internal.bridge

import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeEnvelopeTest {

  @Test
  fun `unwrap returns payload for wrapped message`() {
    val payload = "hello"
    val wrapped = wrapBridgeMessage(payload)
    assertEquals(payload, unwrapBridgeMessage(wrapped))
  }

  @Test
  fun `wrap+unwrap roundtrips for edge-case payloads`() {
    val payloads = listOf(
      "",
      ":",
      "a:b:c",
      "__CMP_WEBVIEW_BRIDGE_V1__:",
      "line1\nline2",
      "emoji-👍",
      "json:{\"a\":1}",
    )

    payloads.forEach { payload ->
      assertEquals(payload, unwrapBridgeMessage(wrapBridgeMessage(payload)))
    }
  }

  @Test
  fun `unwrap returns raw for non-enveloped message`() {
    val raw = "plain"
    assertEquals(raw, unwrapBridgeMessage(raw))
  }

  @Test
  fun `unwrap returns raw for malformed envelope`() {
    val raw = "$BRIDGE_ENVELOPE_PREFIX-nope:payload"
    assertEquals(raw, unwrapBridgeMessage(raw))
  }

  @Test
  fun `unwrap returns raw for length mismatch`() {
    val raw = "${BRIDGE_ENVELOPE_PREFIX}999:short"
    assertEquals(raw, unwrapBridgeMessage(raw))
  }

  @Test
  fun `unwrap returns raw for negative length`() {
    val raw = "${BRIDGE_ENVELOPE_PREFIX}-1:payload"
    assertEquals(raw, unwrapBridgeMessage(raw))
  }
}
