@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
  alias(libs.plugins.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.compose)
  alias(libs.plugins.android.application)
}

kotlin {
  jvmToolchain(17)

  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
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
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(libs.compose.materialIconsExtended)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }

    jvmTest.dependencies {
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
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

android {
  namespace = "sample.app"
  compileSdk = 35

  defaultConfig {
    minSdk = 21
    targetSdk = 35

    applicationId = "sample.app"
    versionCode = 1
    versionName = "1.0.0"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
}

dependencies {
  androidTestImplementation(libs.ui.test.junit4)
  debugImplementation(libs.ui.test.manifest)
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
  // works for both testDebugUnitTest & testReleaseUnitTest
  if (name.endsWith("UnitTest")) {
    exclude("**/*UITest*")
  }
}
