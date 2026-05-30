# ProGuard rules for Desktop WebView (Wry via ComposeNativeWebview)
# Required to prevent obfuscation of JNA and native bridge classes

# Keep JNA native access classes used by Wry
-keep class com.sun.jna.** { *; }

# Keep kotlinx.coroutines.swing for Compose Desktop dispatcher
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory

