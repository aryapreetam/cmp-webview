package io.github.aryapreetam.cmpwebview.internal.constants

/**
 * Unified JavaScript bridge script injected into all web content.
 * Provides consistent ComposeWebViewBridge API across all platforms.
 */
internal const val BRIDGE_SCRIPT = """
(function() {
    // Prevent double initialization
    if (window.ComposeWebViewBridge) {
        console.log('ComposeWebViewBridge already initialized');
        return;
    }
    
    // Message size limit: 10MB
    const MAX_MESSAGE_SIZE = 10 * 1024 * 1024;
    
    /**
     * Sanitize message to prevent injection attacks.
     * Removes potentially dangerous characters and validates size.
     */
    function sanitizeMessage(message) {
        let messageStr = typeof message === 'string' ? message : JSON.stringify(message);
        
        // Check size limit
        if (messageStr.length > MAX_MESSAGE_SIZE) {
            console.error('Message exceeds 10MB limit, truncating...');
            messageStr = messageStr.substring(0, MAX_MESSAGE_SIZE);
        }
        
        // Basic sanitization: escape control characters
        messageStr = messageStr.replace(/[\x00-\x1F\x7F]/g, '');
        
        return messageStr;
    }
    
    /**
     * Unified bridge object exposed to web content.
     */
    window.ComposeWebViewBridge = {
        /**
         * Send message from JavaScript to Compose.
         * Automatically detects platform and routes to appropriate native bridge.
         * 
         * @param {string|object} message - Message to send (will be stringified if object)
         */
        postMessage: function(message) {
            const sanitizedMessage = sanitizeMessage(message);
            
            // Try Android bridge
            if (window.AndroidBridge && typeof window.AndroidBridge.postMessage === 'function') {
                try {
                    window.AndroidBridge.postMessage(sanitizedMessage);
                    return;
                } catch (e) {
                    console.error('Android bridge error:', e);
                }
            }
            
            // Try iOS bridge
            if (window.webkit && 
                window.webkit.messageHandlers && 
                window.webkit.messageHandlers.iosBridge) {
                try {
                    window.webkit.messageHandlers.iosBridge.postMessage(sanitizedMessage);
                    return;
                } catch (e) {
                    console.error('iOS bridge error:', e);
                }
            }
            
            // Try Desktop (JavaFX) bridge
            if (window.javaBridge && typeof window.javaBridge.postMessage === 'function') {
                try {
                    window.javaBridge.postMessage(sanitizedMessage);
                    return;
                } catch (e) {
                    console.error('Desktop bridge error:', e);
                }
            }
            
            // Try Web/WASM bridge (postMessage to parent)
            if (window.parent && window.parent !== window) {
                try {
                    window.parent.postMessage(sanitizedMessage, '*');
                    return;
                } catch (e) {
                    console.error('Web bridge error:', e);
                }
            }
            
            console.warn('No bridge available on this platform');
        }
    };
    
    // Dispatch ready event
    const readyEvent = new Event('ComposeWebViewBridgeReady');
    window.dispatchEvent(readyEvent);
    
    console.log('ComposeWebViewBridge initialized successfully');
})();
"""

