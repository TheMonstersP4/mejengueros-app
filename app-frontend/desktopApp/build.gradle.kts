import org.jetbrains.compose.desktop.application.dsl.TargetFormat

fun detectMapLibreDesktopTarget(): String {
  val hostOs =
      when (val os = System.getProperty("os.name").lowercase()) {
        "mac os x" -> "macos"
        else -> os.split(" ").first()
      }
  val hostArch =
      when (val arch = System.getProperty("os.arch").lowercase()) {
        "x86_64" -> "amd64"
        "arm64" -> "aarch64"
        else -> arch
      }
  val renderer = if (hostOs == "macos") "metal" else "opengl"

  return "$hostOs-$hostArch-$renderer"
}

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

dependencies {
  implementation(projects.shared)

  implementation(compose.desktop.currentOs)
  implementation(libs.kotlinx.coroutinesSwing)
  runtimeOnly(libs.maplibre.native.bindings.jni) {
    capabilities {
      requireCapability(
          "org.maplibre.compose:maplibre-native-bindings-jni-${detectMapLibreDesktopTarget()}"
      )
    }
  }

  implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
  application {
    mainClass = "io.github.themonstersp4.mejengueros.MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "io.github.themonstersp4.mejengueros"
      packageVersion = "1.0.0"
    }
  }
}
