# cmp-webview sample app

## Desktop (JVM)

Recommended command:

```bash
./gradlew :sample:composeApp:jvmRun -DmainClass=MainKt
```

If you run into a Desktop hang where logs show `CefApp: set state INITIALIZING` and never reach `INITIALIZED`, it’s usually due to the Gradle JVM / JBR / JCEF wiring (especially from composite builds / IDE run configs). In that case:

- Prefer `jvmRun` over `run`
- Ensure Gradle is using JetBrains Runtime (JBR) that includes JCEF
