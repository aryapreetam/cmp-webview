package io.github.aryapreetam.cmpwebview.internal.bridge

/**
 * Very small, dependency-free message envelope.
 *
 * Why not JSON?
 * - We want to avoid adding JSON parsing dependencies in common code.
 * - We want a robust framing format that survives arbitrary payload content.
 *
 * Format: `${BRIDGE_ENVELOPE_PREFIX}<len>:<payload>`
 */
internal const val BRIDGE_ENVELOPE_PREFIX = "__CMP_WEBVIEW_BRIDGE_V1__:"

internal fun wrapBridgeMessage(payload: String): String {
  return "$BRIDGE_ENVELOPE_PREFIX${payload.length}:$payload"
}

internal fun unwrapBridgeMessage(raw: String): String {
  if (!raw.startsWith(BRIDGE_ENVELOPE_PREFIX)) return raw

  val afterPrefix = raw.substring(BRIDGE_ENVELOPE_PREFIX.length)
  val sepIndex = afterPrefix.indexOf(':')
  if (sepIndex <= 0) return raw

  val expectedLen = afterPrefix.substring(0, sepIndex).toIntOrNull() ?: return raw
  if (expectedLen < 0) return raw

  val payloadStart = BRIDGE_ENVELOPE_PREFIX.length + sepIndex + 1
  if (payloadStart > raw.length) return raw

  val payload = raw.substring(payloadStart)
  return if (payload.length == expectedLen) payload else raw
}
