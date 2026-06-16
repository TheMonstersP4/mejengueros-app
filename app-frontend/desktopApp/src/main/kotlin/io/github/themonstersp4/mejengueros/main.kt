package io.github.themonstersp4.mejengueros

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.themonstersp4.mejengueros.app.App
import io.github.themonstersp4.mejengueros.di.initKoin

fun main() {
  initKoin()

  application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Mejengueros",
    ) {
      App()
    }
  }
}
