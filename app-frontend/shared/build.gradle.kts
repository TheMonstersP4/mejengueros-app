import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinxSerialization)
  alias(libs.plugins.sqlDelight)
}

kotlin {
  listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  jvm()

  androidLibrary {
    namespace = "io.github.themonstersp4.mejengueros.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    androidResources { enable = true }
    withHostTest { isIncludeAndroidResources = true }
  }

  sourceSets {
    named("androidHostTest").dependencies {
      implementation(libs.androidx.compose.uiTestJunit4)
      implementation(libs.androidx.compose.uiTestManifest)
      implementation(libs.androidx.test.core)
      implementation(libs.robolectric)
    }
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.security.crypto)
      implementation(libs.koin.android)
      implementation(libs.ktor.client.okhttp)
      implementation(libs.sqldelight.driver.android)
    }
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.material.icons.core)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.cryptography.random)
      implementation(libs.kotlincrypto.sha2)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
      implementation(libs.coil.compose)
      implementation(libs.coil.network.ktor3)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.jetbrains.navigation3.ui)
      implementation(libs.kotlinx.serialization.core)
      implementation(project.dependencies.platform(libs.koin.bom))
      implementation(libs.koin.core)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.ktor.client.mock)
    }
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
      implementation(libs.sqldelight.driver.native)
    }
    jvmMain.dependencies {
      implementation(libs.ktor.client.okhttp)
      implementation(libs.sqldelight.driver.sqlite)
    }
  }
}

sqldelight {
  databases {
    create("AppDatabase") { packageName.set("io.github.themonstersp4.mejengueros.data.local") }
  }
  linkSqlite = true
}

compose.resources { packageOfResClass = "io.github.themonstersp4.mejengueros.generated.resources" }

dependencies { androidRuntimeClasspath(libs.compose.uiTooling) }
