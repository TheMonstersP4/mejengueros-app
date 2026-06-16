package io.github.themonstersp4.mejengueros.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

object AuthCallbackBus {
  private val _callbackUrls = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
  val callbackUrls: SharedFlow<String> = _callbackUrls

  fun publish(callbackUrl: String) {
    _callbackUrls.tryEmit(callbackUrl)
  }
}

fun handleOAuthCallback(url: String) {
  AuthCallbackBus.publish(url)
}
