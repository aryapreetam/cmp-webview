@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.multiplatform)
  alias(libs.plugins.android.library)
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.compose)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.dokka)
  alias(libs.plugins.os.detector)
}

kotlin {
  jvmToolchain(17)

  androidTarget { publishLibraryVariants("release") }
  jvm()
  wasmJs { browser() }
  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.ui)
      implementation(compose.foundation)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
    }
    androidMain.dependencies {
      implementation(libs.compose.webview)
    }
    jvmMain.dependencies {
      val fxSuffix = when (osdetector.classifier) {
        "linux-x86_64" -> "linux"
        "linux-aarch_64" -> "linux-aarch64"
        "windows-x86_64" -> "win"
        "osx-x86_64" -> "mac"
        "osx-aarch_64" -> "mac-aarch64"
        else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
      }
      implementation("org.openjfx:javafx-base:19:${fxSuffix}")
      implementation("org.openjfx:javafx-graphics:19:${fxSuffix}")
      implementation("org.openjfx:javafx-controls:19:${fxSuffix}")
      implementation("org.openjfx:javafx-swing:19:${fxSuffix}")
      implementation("org.openjfx:javafx-web:19:${fxSuffix}")
      implementation("org.openjfx:javafx-media:19:${fxSuffix}")
      implementation(libs.kotlinx.coroutines.swing)
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

android {
  namespace = "io.github.aryapreetam.cmpwebview"
  compileSdk = 35

  defaultConfig {
    minSdk = 21
  }
}

dependencies {
  dokkaPlugin(libs.android.documentation.plugin)
}

//Publishing your Kotlin Multiplatform library to Maven Central
//https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
mavenPublishing {
  publishToMavenCentral()
  coordinates("io.github.aryapreetam", "cmp-webview", "0.0.1")

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
