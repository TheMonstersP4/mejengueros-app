package io.github.themonstersp4.mejengueros.data.auth

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosOAuthBrowser : IOAuthBrowser {
  override suspend fun open(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: error("Invalid OAuth URL.")
    UIApplication.sharedApplication.openURL(nsUrl)
  }
}
