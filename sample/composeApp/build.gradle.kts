@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  alias(libs.plugins.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.compose)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  jvmToolchain(17)

  androidLibrary {
    namespace = "sample.app"
    compileSdk = 35
    minSdk = 23
  }
  jvm()
  wasmJs {
    browser()
    binaries.executable()
  }
  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.ui.multiplatform)
      implementation(libs.compose.material3)
      implementation(libs.compose.foundation)
      implementation(project(":lib"))
      implementation(libs.compose.materialIconsExtended)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }

    jvmTest.dependencies {
      implementation(libs.compose.ui.test)
    }

    androidMain.dependencies {
      implementation(libs.androidx.activityCompose)
    }

    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation("org.slf4j:slf4j-nop:2.0.13")
    }
  }
}

compose.desktop {
  application {
    mainClass = "MainKt"

    // Add JVM arguments required for KCEF
    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

    if (System.getProperty("os.name").contains("Mac")) {
      jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
      jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
    }

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "sample"
      packageVersion = "1.0.0"
    }

    buildTypes.release.proguard {
      configurationFiles.from("compose-desktop.pro")
    }
  }
}

tasks.withType<Test>().configureEach {
  // Disable headless mode so Swing components function under xvfb-run
  systemProperty("java.awt.headless", "false")
  systemProperty("cmpwebview.testmode", "true")

  // Pass necessary opens parameters for JVM modular reflection in JCEF
  jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
  if (System.getProperty("os.name").contains("Mac")) {
    jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
  }

  // works for both testDebugUnitTest & testReleaseUnitTest
  if (name.endsWith("UnitTest")) {
    exclude("**/*UITest*")
  }

  // Exclude the heavy, headless-unstable JCEF Swing integration test from CI
  if (System.getenv("GITHUB_ACTIONS") == "true") {
    exclude("**/WebViewBridgeIntegrationJvmTest*")
  }
}
