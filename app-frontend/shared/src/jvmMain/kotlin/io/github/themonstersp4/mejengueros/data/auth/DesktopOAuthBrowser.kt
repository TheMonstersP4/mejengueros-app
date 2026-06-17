package io.github.themonstersp4.mejengueros.data.auth

import java.awt.Desktop
import java.net.URI

class DesktopOAuthBrowser : IOAuthBrowser {
  override suspend fun open(url: String) {
    check(Desktop.isDesktopSupported()) { "Desktop browser is not available." }
    Desktop.getDesktop().browse(URI(url))
  }
}
