@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
  alias(libs.plugins.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dokka)
}

kotlin {
  jvmToolchain(17)

  androidLibrary {
    namespace = "io.github.aryapreetam.cmpwebview"
    compileSdk = 35
    minSdk = 23
    withHostTest {  }
    androidResources {
      enable = true
    }
  }
  jvm()
  wasmJs { browser() }
  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.ui.multiplatform)
      implementation(libs.compose.foundation)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
    }
    androidMain.dependencies {
      implementation(libs.compose.webview)
    }
    jvmMain.dependencies {
      implementation(libs.compose.native.webview)
      implementation(libs.kotlinx.coroutines.swing)
    }

    jvmTest.dependencies {
      implementation(kotlin("test"))
    }
  }

  //https://kotlinlang.org/docs/native-objc-interop.html#export-of-kdoc-comments-to-generated-objective-c-headers
  targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    compilations["main"].compileTaskProvider.configure {
      compilerOptions {
        freeCompilerArgs.add("-Xexport-kdoc")
      }
    }
  }
}

// NOTE: Host-specific dependency leakage guardrail:
// DO NOT import host-specific binary dependencies (e.g. `compose.desktop.currentOs`) under library targets.
// Any desktop UI implementation should target standard platform-agnostic `jvm()` targets.
// Platform-specific runtime locators must be restricted solely to the executable sample application (:sample).

dependencies {
  dokkaPlugin(libs.android.documentation.plugin)
}

// Configure Dokka V2 extension
dokka {
  moduleName.set("cmp-webview")

  dokkaPublications.html {
    suppressObviousFunctions.set(false)
    suppressInheritedMembers.set(false)
  }

  dokkaSourceSets.configureEach {
    // Include Module.md from the source directory
    includes.from("src/commonMain/kotlin/Module.md")
    includes.from("src/commonMain/kotlin/io/github/aryapreetam/cmpwebview/package.md")

    // Add module documentation
//    displayName.set("Common")

    // Add source links to GitHub
    sourceLink {
      localDirectory.set(file("src"))
      remoteUrl("https://github.com/aryapreetam/cmp-webview/tree/main/lib/src")
      remoteLineSuffix.set("#L")
    }

    // Suppress internal packages
    perPackageOption {
      matchingRegex.set(".*\\.internal.*")
      suppress.set(true)
    }

    // Add package documentation
    perPackageOption {
      matchingRegex.set("io.github.aryapreetam.cmpwebview")
      reportUndocumented.set(true)
      skipDeprecated.set(false)
    }
  }
}

//Publishing your Kotlin Multiplatform library to Maven Central
//https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
mavenPublishing {
  publishToMavenCentral()
  coordinates(
      project.group.toString(),
      findProperty("libArtifactId")?.toString() ?: "cmp-webview",
      project.version.toString()
  )

  pom {
    name = "Compose Multiplatform WebView"
    description = "Simple WebView for Compose Multiplatform"
    url = "https://aryapreetam.github.io/cmp-webview" //todo

    licenses {
      license {
        name = "MIT"
        url = "https://opensource.org/licenses/MIT"
      }
    }

    developers {
      developer {
        id = "aryapreetam" //todo
        name = "Preetam Bhosle" //todo
      }
    }

    scm {
      url = "https://github.com/aryapreetam/cmp-webview" //todo
    }
  }
  // Sign publications if either local keyId or CI signingInMemoryKey is available
  if (project.hasProperty("signing.keyId") || project.hasProperty("signingInMemoryKey")) {
    signAllPublications()
  }
}

tasks.withType<Test>().configureEach {
  // Disable headless mode so Swing components function under xvfb-run
  systemProperty("java.awt.headless", "false")
  systemProperty("cmpwebview.testmode", "true")

  jvmArgs("--enable-native-access=ALL-UNNAMED")
}

