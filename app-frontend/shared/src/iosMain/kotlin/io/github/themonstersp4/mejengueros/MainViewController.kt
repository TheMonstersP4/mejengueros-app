package io.github.themonstersp4.mejengueros

import androidx.compose.ui.window.ComposeUIViewController
import io.github.themonstersp4.mejengueros.app.App
import io.github.themonstersp4.mejengueros.di.initKoin

fun MainViewController() = ComposeUIViewController {
  initKoin()
  App()
}
