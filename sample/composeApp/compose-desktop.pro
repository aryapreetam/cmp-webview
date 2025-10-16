# ProGuard rules for KCEF (Kotlin Chromium Embedded Framework)
# Required to prevent obfuscation of CEF classes and coroutines

# Keep all CEF classes
-keep class org.cef.** { *; }

# Keep kotlinx.coroutines.swing for KCEF initialization
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory

