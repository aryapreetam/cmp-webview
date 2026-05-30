# cmp-webview sample app

## Desktop (JVM)

Recommended command:

```bash
./gradlew :sample:composeApp:run
```

If the Desktop app hangs on startup, try using `jvmRun` instead and ensure Gradle is using JetBrains Runtime (JBR):

- `./gradlew :sample:composeApp:jvmRun -DmainClass=MainKt`
